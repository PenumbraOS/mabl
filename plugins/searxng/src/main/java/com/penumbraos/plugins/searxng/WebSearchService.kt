package com.penumbraos.plugins.searxng

import android.util.Log
import com.penumbraos.mabl.sdk.IToolCallback
import com.penumbraos.mabl.sdk.ToolCall
import com.penumbraos.mabl.sdk.ToolDefinition
import com.penumbraos.mabl.sdk.ToolParameter
import com.penumbraos.mabl.sdk.ToolService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val TAG = "WebSearchService"
private const val WEB_SEARCH = "web_search"

class WebSearchService : ToolService("WebSearchService") {

    private val searchScope = CoroutineScope(Dispatchers.IO)
    private lateinit var searxngClient: SearxngClient
    private val resultProcessor = SearchResultProcessor()

    override fun onCreate() {
        super.onCreate()
        searxngClient = SearxngClient(this)
    }

    override fun executeTool(call: ToolCall, params: JSONObject?, callback: IToolCallback) {
        when (call.name) {
            WEB_SEARCH -> {
                try {
                    val parametersJson = JSONObject(call.parameters)
                    val query = parametersJson.optString("query", "").trim()

                    if (query.isEmpty()) {
                        Log.w(TAG, "Empty search query provided")
                        callback.onError("Search query cannot be empty")
                        return
                    }

                    val category = parametersJson.optString("category", "general")
                    var timeRange = parametersJson.optString("time_range", "")

                    if (timeRange == "all") {
                        timeRange = ""
                    }

                    Log.d(TAG, "Executing web search for: '$query'")

                    searchScope.launch {
                        try {
                            val engines = determineOptimalEngines(query, category)

                            val response = searxngClient.search(
                                query = query,
                                categories = category,
                                engines = engines,
                                maxResults = 3,
                                timeRange = timeRange
                            )

                            if (response != null && response.results.isNotEmpty()) {
                                val processedResults = resultProcessor.processResults(response)
                                Log.d(TAG, "Search successful: ${response.results.size} results")
                                callback.onSuccess(processedResults)
                            } else {
                                Log.w(TAG, "No valid results found for query: $query")
                                callback.onError("No results found for '$query'")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Search failed for query: $query", e)
                            callback.onError("Search failed: ${e.message ?: "Unknown error"}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse search parameters", e)
                    callback.onError("Invalid search parameters: ${e.message}")
                }
            }

            else -> {
                Log.w(TAG, "Unknown tool: ${call.name}")
                callback.onError("Unknown tool: ${call.name}")
            }
        }
    }

    override fun getToolDefinitions(): Array<ToolDefinition> {
        return arrayOf(
            ToolDefinition().apply {
                name = WEB_SEARCH
                description =
                    "Search the web for current information and return structured results. Use this tool for current events, news, technical information, articles, and knowledge lookups. Returns JSON with search results including title, URL, content, and source for each result."
                isPriority = true
                parameters = arrayOf(
                    ToolParameter().apply {
                        name = "query"
                        type = "string"
                        description = "The search query to execute"
                        required = true
                        enumValues = emptyArray()
                    },
                    ToolParameter().apply {
                        name = "category"
                        type = "string"
                        description =
                            "Search category (general, news, images, videos, it, science, etc.)"
                        required = false
                        enumValues = arrayOf(
                            "general",
                            "news",
                            "images",
                            "videos",
                            "it",
                            "science",
                            "social_media"
                        )
                    },
                    ToolParameter().apply {
                        name = "time_range"
                        type = "string"
                        description = "Time range for results (day, week, month, year)"
                        required = false
                        enumValues = arrayOf("all", "day", "week", "month", "year")
                    }
                )
            }
        )
    }

    private fun determineOptimalEngines(query: String, category: String): String {
        val queryLower = query.lowercase()

        return when {
            // Wikipedia for factual/definition queries
            queryLower.startsWith("what is") ||
                    queryLower.startsWith("who is") ||
                    queryLower.startsWith("define") ||
                    queryLower.contains("definition") -> "wikipedia,duckduckgo,bing"

            // News category or current events
            category == "news" ||
                    queryLower.contains("news") ||
                    queryLower.contains("latest") ||
                    queryLower.contains("today") -> "duckduckgo,bing"

            // Technical/programming queries
            category == "it" ||
                    queryLower.contains("programming") ||
                    queryLower.contains("code") ||
                    queryLower.contains("software") -> "duckduckgo,bing"

            // General search - balanced mix
            else -> "duckduckgo,bing,wikipedia"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        searxngClient.close()
    }
}