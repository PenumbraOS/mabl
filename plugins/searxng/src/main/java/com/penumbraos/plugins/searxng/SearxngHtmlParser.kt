package com.penumbraos.plugins.searxng

import android.util.Log
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.regex.Pattern

private const val TAG = "SearxngHtmlParser"

data class HtmlSearchResult(
    val url: String,
    val title: String,
    val summary: String
)

class SearxngHtmlParser {

    companion object {
        // Pattern to detect if we're on the main/index page
        private val INDEX_PAGE_PATTERN = Pattern.compile("body\\s+class=[\"'].*index_endpoint.*[\"']", Pattern.CASE_INSENSITIVE)
        
        // Various selectors for different SearXNG themes/versions
        private val RESULT_SELECTORS = listOf(
            "article.result",           // Most common
            ".result",                  // Alternative
            "div.result",              // Fallback
            "[class*=result]"          // Broad match
        )
        
        private val URL_SELECTORS = listOf(
            "a.url_header",            // Primary URL selector
            ".result h3 a",            // Alternative header link
            ".result-title a",         // Another variant
            ".result .title a",        // Generic title link
            "h3 a[href]",             // Fallback header link
            "a[href*='http']"         // Any external link
        )
        
        private val SUMMARY_SELECTORS = listOf(
            "p.content",               // Most common summary
            ".result-content",         // Alternative content
            ".result .content",        // Generic content
            ".snippet",                // Snippet text
            ".description",            // Description text
            "p"                        // Fallback paragraph
        )
    }

    fun parseResults(html: String): List<HtmlSearchResult> {
        try {
            // Check if we're redirected to index page
            if (INDEX_PAGE_PATTERN.matcher(html).find()) {
                Log.w(TAG, "Detected redirect to index page")
                return emptyList()
            }

            val document = Jsoup.parse(html)
            Log.d(TAG, "Parsing HTML document with ${document.text().length} characters")

            // Try different result selectors
            for (selector in RESULT_SELECTORS) {
                val results = parseWithSelector(document, selector)
                if (results.isNotEmpty()) {
                    Log.d(TAG, "Successfully parsed ${results.size} results with selector: $selector")
                    return results
                }
            }

            Log.w(TAG, "No results found with any selector")
            return emptyList()

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing HTML results", e)
            return emptyList()
        }
    }

    private fun parseWithSelector(document: Document, resultSelector: String): List<HtmlSearchResult> {
        val results = mutableListOf<HtmlSearchResult>()
        val resultElements = document.select(resultSelector)
        
        Log.d(TAG, "Found ${resultElements.size} result elements with selector: $resultSelector")

        for (element in resultElements) {
            val result = parseResultElement(element)
            if (result != null) {
                results.add(result)
            }
        }

        return results
    }

    private fun parseResultElement(element: Element): HtmlSearchResult? {
        // Extract URL
        val url = extractUrl(element) ?: return null
        
        // Skip if it's not a valid external URL
        if (!isValidUrl(url)) {
            Log.d(TAG, "Skipping invalid URL: $url")
            return null
        }

        // Extract title
        val title = extractTitle(element, url)

        // Extract summary
        val summary = extractSummary(element)

        return HtmlSearchResult(
            url = cleanUrl(url),
            title = title,
            summary = summary
        )
    }

    private fun extractUrl(element: Element): String? {
        for (selector in URL_SELECTORS) {
            val linkElement = element.selectFirst(selector)
            if (linkElement != null) {
                val href = linkElement.attr("href")
                if (href.isNotBlank()) {
                    Log.d(TAG, "Found URL with selector $selector: $href")
                    return href
                }
            }
        }
        return null
    }

    private fun extractTitle(element: Element, fallbackUrl: String): String {
        // Try to get title from link text
        for (selector in URL_SELECTORS) {
            val linkElement = element.selectFirst(selector)
            if (linkElement != null) {
                val title = linkElement.text().trim()
                if (title.isNotBlank()) {
                    return title
                }
            }
        }

        // Fallback to any heading text
        val heading = element.selectFirst("h1, h2, h3, h4, h5, h6")
        if (heading != null) {
            val title = heading.text().trim()
            if (title.isNotBlank()) {
                return title
            }
        }

        // Last resort - use domain from URL
        return try {
            val domain = fallbackUrl.substringAfter("://").substringBefore("/")
            domain.removePrefix("www.")
        } catch (e: Exception) {
            "Search Result"
        }
    }

    private fun extractSummary(element: Element): String {
        for (selector in SUMMARY_SELECTORS) {
            val summaryElement = element.selectFirst(selector)
            if (summaryElement != null) {
                val text = summaryElement.text().trim()
                if (text.isNotBlank() && text.length > 10) {
                    return cleanSummaryText(text)
                }
            }
        }

        // Fallback to any text content
        val text = element.text().trim()
        return if (text.length > 20) {
            cleanSummaryText(text.take(200))
        } else {
            "No summary available"
        }
    }

    private fun cleanSummaryText(text: String): String {
        return text
            .replace(Regex("\\s+"), " ")  // Normalize whitespace
            .replace(Regex("\\[\\d+\\]"), "")  // Remove reference numbers
            .replace("...", "")
            .trim()
    }

    private fun isValidUrl(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }

    private fun cleanUrl(url: String): String {
        // Remove SearXNG redirect wrapper if present
        return when {
            url.contains("/url?") -> {
                // Extract the actual URL from redirect parameters
                val urlParam = url.substringAfter("url=").substringBefore("&")
                try {
                    java.net.URLDecoder.decode(urlParam, "UTF-8")
                } catch (e: Exception) {
                    url
                }
            }
            url.contains("uddg=") -> {
                // DuckDuckGo-style redirect
                url.substringAfter("uddg=").substringBefore("&")
            }
            else -> url
        }
    }

    fun hasValidResults(html: String): Boolean {
        return try {
            !INDEX_PAGE_PATTERN.matcher(html).find() && 
            html.length > 100 && 
            parseResults(html).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}