package com.penumbraos.plugins.searxng

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val TAG = "SearchResultProcessor"

@Serializable
data class CleanedSearchResult(
    val title: String,
    val url: String,
    val content: String,
    val source: String
)

@Serializable
data class SearchResponse(
    val query: String,
    val results: List<CleanedSearchResult>
)

class SearchResultProcessor {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = false
    }

    fun processResults(response: SearxngResponse): String {
        Log.d(TAG, "Processing ${response.results.size} search results")

        if (response.results.isEmpty()) {
            return "No search results found for query: '${response.query}'"
        }

        val cleanedResults = response.results.map { result ->
            CleanedSearchResult(
                title = cleanText(result.title),
                url = result.url,
                content = cleanText(result.content),
                source = extractDomain(result.url)
            )
        }

        val searchResponse = SearchResponse(
            query = response.query,
            results = cleanedResults
        )

        return json.encodeToString(SearchResponse.serializer(), searchResponse)
    }

    private fun cleanText(text: String): String {
        if (text.isBlank()) return ""

        return text
            // Remove HTML tags
            .replace(Regex("<[^>]*>"), "")
            // Remove excessive whitespace
            .replace(Regex("\\s+"), " ")
            // Remove common web artifacts
            .replace(Regex("\\[\\d+\\]"), "") // Wikipedia reference numbers
            .replace("...", "")
            .replace("Read more", "")
            .replace("Continue reading", "")
            // Fix common HTML entities
            .replace("&amp;", "and")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            // Remove extra punctuation artifacts
            .replace(Regex("\\.{2,}"), ".")
            .replace(Regex("\\s*[|•]\\s*"), " - ")
            .trim()
    }

    private fun extractDomain(url: String): String {
        return try {
            if (url.isBlank()) return "unknown"

            val cleanUrl = if (!url.startsWith("http")) "https://$url" else url
            val domain = java.net.URL(cleanUrl).host.lowercase()

            // Return clean domain name
            domain.removePrefix("www.")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract domain from: $url", e)
            "unknown"
        }
    }
}