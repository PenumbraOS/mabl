package com.penumbraos.plugins.googlesearch

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL

private const val TAG = "GoogleSearchProcessor"

@Serializable
data class ProcessedSearchResult(
    val title: String,
    val url: String,
    val snippet: String,
    val displayLink: String
)

@Serializable
data class ProcessedSearchResponse(
    val query: String,
    val totalResults: String,
    val searchTime: String,
    val results: List<ProcessedSearchResult>
)

class GoogleSearchProcessor {

    private val json = Json { 
        prettyPrint = true 
        encodeDefaults = false
    }

    fun processResults(response: GoogleSearchResponse, originalQuery: String): String {
        Log.d(TAG, "Processing Google Search results")

        val items = response.items ?: emptyList()
        
        if (items.isEmpty()) {
            return "No search results found for query: '$originalQuery'"
        }

        val processedResults = items.map { item ->
            ProcessedSearchResult(
                title = cleanText(item.title),
                url = item.link,
                snippet = cleanText(item.snippet),
                displayLink = item.displayLink
            )
        }

        val searchInfo = response.searchInformation
        val processedResponse = ProcessedSearchResponse(
            query = originalQuery,
            totalResults = searchInfo?.formattedTotalResults ?: searchInfo?.totalResults ?: "Unknown",
            searchTime = searchInfo?.formattedSearchTime ?: "${searchInfo?.searchTime ?: 0.0} seconds",
            results = processedResults
        )

        return json.encodeToString(ProcessedSearchResponse.serializer(), processedResponse)
    }

    private fun cleanText(text: String): String {
        if (text.isBlank()) return ""
        
        return text
            // Remove HTML tags
            .replace(Regex("<[^>]*>"), "")
            // Remove excessive whitespace
            .replace(Regex("\\s+"), " ")
            // Remove common artifacts
            .replace("...", "")
            .replace(" ...", "")
            .replace("... ", "")
            // Fix HTML entities
            .replace("&amp;", "and")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
            // Clean up dates and formatting
            .replace(Regex("\\s*-\\s*$"), "") // Remove trailing dash
            .replace(Regex("^\\s*-\\s*"), "") // Remove leading dash
            .trim()
    }

    fun hasValidResults(response: GoogleSearchResponse): Boolean {
        return !response.items.isNullOrEmpty()
    }
}