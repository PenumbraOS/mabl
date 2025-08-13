package com.penumbraos.plugins.searxng

import android.content.Context
import android.util.Log
import com.penumbraos.sdk.PenumbraClient
import com.penumbraos.sdk.http.ktor.HttpClientPlugin
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable

private const val TAG = "SearxngClient"

@Serializable
data class SearxngResponse(
    val query: String = "",
    val number_of_results: Int = 0,
    val results: List<SearchResult> = emptyList(),
    val answers: List<String> = emptyList(),
    val corrections: List<String> = emptyList(),
    val infoboxes: List<Infobox> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val unresponsive_engines: List<List<String>> = emptyList()
)

@Serializable
data class SearchResult(
    val url: String,
    val title: String,
    val content: String = "",
    val engine: String = "",
    val parsed_url: List<String> = emptyList(),
    val template: String = "",
    val engines: List<String> = emptyList(),
    val positions: List<Int> = emptyList(),
    val score: Double = 0.0,
    val category: String = ""
)

@Serializable
data class Infobox(
    val infobox: String = "",
    val content: String = "",
    val engine: String = "",
    val engines: List<String> = emptyList()
)

class SearxngClient(
    context: Context,
    private val instances: List<String> = DEFAULT_INSTANCES
) {
    companion object {
        private val DEFAULT_INSTANCES = listOf(
            "https://search.bus-hit.me",
            "https://searx.tiekoetter.com",
            "https://search.sapti.me",
            "https://searx.be",
            "https://searx.perennialte.ch"
        )

        private const val REQUEST_TIMEOUT_MS = 5000L
        private const val FALLBACK_TIMEOUT_MS = 3000L
    }

    private val penumbra = PenumbraClient(context)
    private val htmlParser = SearxngHtmlParser()

    private val httpClient = HttpClient(Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            connectTimeoutMillis = 3000
            socketTimeoutMillis = REQUEST_TIMEOUT_MS
        }
        install(UserAgent) {
            agent =
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36"
        }
        install(HttpClientPlugin) {
            penumbraClient = penumbra
        }
        defaultRequest {
            header(
                "Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
            )
            header("Accept-Language", "en-US,en;q=0.5")
        }
    }

    suspend fun search(
        query: String,
        categories: String = "general",
        engines: String = "duckduckgo,bing,wikipedia",
        maxResults: Int = 3,
        safeSearch: Int = 1,
        timeRange: String = ""
    ): SearxngResponse? {
        if (query.isBlank()) {
            Log.w(TAG, "Empty query provided")
            return null
        }

        Log.d(TAG, "Searching for: '$query' with engines: $engines")

        // Shuffle instances to distribute load
        val shuffledInstances = instances.shuffled()

        for (instance in shuffledInstances) {
            try {
                Log.d(TAG, "Trying instance: $instance")

                val response = withTimeoutOrNull(
                    if (instance == shuffledInstances.first()) REQUEST_TIMEOUT_MS else FALLBACK_TIMEOUT_MS
                ) {
                    httpClient.get("$instance/search") {
                        parameter("q", query.trim())
                        parameter("categories", categories)
                        if (engines.isNotBlank()) {
                            parameter("engines", engines)
                        }
                        parameter("safesearch", safeSearch)
                        if (timeRange.isNotBlank()) {
                            parameter("time_range", timeRange)
                        }
                        // Headers to look more like a real browser
                        header("Referer", instance)
                        header(
                            "Accept",
                            "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8"
                        )
                        header("Accept-Language", "en-US,en;q=0.9")
                        header("DNT", "1")
                        header("Connection", "keep-alive")
                        header("Upgrade-Insecure-Requests", "1")
                        header("Sec-Fetch-Dest", "document")
                        header("Sec-Fetch-Mode", "navigate")
                        header("Sec-Fetch-Site", "same-origin")
                    }
                }

                if (response != null) {
                    val html = response.bodyAsText()

                    if (html.length < 200) {
                        Log.w(TAG, "Response too short from $instance: ${html.length} chars")
                        continue
                    }

                    // Check if we were redirected to index page
                    if (!htmlParser.hasValidResults(html)) {
                        Log.w(
                            TAG,
                            "No valid results from $instance - likely bot detection, moving to next instance"
                        )
                        continue
                    }

                    return parseHtmlResults(html, query, maxResults, instance)
                } else {
                    Log.w(TAG, "Null response from $instance")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Search failed for instance $instance: ${e.message}")
                continue
            }
        }

        Log.e(TAG, "All SearXNG instances failed for query: $query")
        return null
    }

    private fun parseHtmlResults(
        html: String,
        query: String,
        maxResults: Int,
        instance: String
    ): SearxngResponse? {
        val htmlResults = htmlParser.parseResults(html)

        if (htmlResults.isEmpty()) {
            Log.w(TAG, "HTML parsing yielded no results from $instance")
            Log.d(TAG, "HTML snippet: ${html.take(500)}")
            return null
        }

        val searchResults = htmlResults.take(maxResults).map { htmlResult ->
            SearchResult(
                url = htmlResult.url,
                title = htmlResult.title,
                content = htmlResult.summary,
                engine = "searxng"
            )
        }

        Log.d(TAG, "HTML parsing successful from $instance: ${searchResults.size} results")

        return SearxngResponse(
            query = query,
            number_of_results = searchResults.size,
            results = searchResults
        )
    }

    suspend fun searchWithFallback(query: String): SearxngResponse {
        // Try general search first
        search(query, "general", "duckduckgo,bing,wikipedia")?.let { return it }

        // Fallback to simpler search
        search(query, "general", "duckduckgo")?.let { return it }

        // Last resort - return empty response
        Log.e(TAG, "All search attempts failed for query: $query")
        return SearxngResponse(
            query = query,
            number_of_results = 0,
            results = emptyList()
        )
    }

    fun close() {
        httpClient.close()
    }
}