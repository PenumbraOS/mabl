package com.penumbraos.plugins.googlesearch

import android.util.Log
import com.penumbraos.mabl.sdk.IToolCallback
import com.penumbraos.mabl.sdk.ToolCall
import com.penumbraos.mabl.sdk.ToolDefinition
import com.penumbraos.mabl.sdk.ToolParameter
import com.penumbraos.mabl.sdk.ToolService
import com.penumbraos.sdk.PenumbraClient
import com.penumbraos.sdk.api.SettingsClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val TAG = "GoogleSearchService"
private const val WEB_SEARCH = "web_search"

private const val SETTING_CATEGORY = "google"
private const val SETTING_API_KEY = "apiKey"
private const val SETTING_SEARCH_ENGINE_ID = "searchEngineId"

class GoogleSearchService : ToolService("GoogleSearchService") {

    private val searchScope = CoroutineScope(Dispatchers.IO)
    private val searchProcessor = GoogleSearchProcessor()
    private lateinit var settings: SettingsClient
    
    override fun onCreate() {
        super.onCreate()
        searchScope.launch {
            val client = PenumbraClient(this@GoogleSearchService)
            client.waitForBridge()
            settings = client.settings
            settings.registerSettings(
                this@GoogleSearchService.packageName,
            ) {
                category(SETTING_CATEGORY) {
                    stringSetting(SETTING_API_KEY, "")
                    stringSetting(SETTING_SEARCH_ENGINE_ID, "")
                }
            }
        }
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

                    val numResults = parametersJson.optInt("num_results", 10).coerceIn(1, 10)
                    val timeRestrictString = parametersJson.optString("time_restrict", "")
                        .takeIf { it.isNotBlank() }
                    val timeRestrict = if (!timeRestrictString.isNullOrBlank()) {
                        DateRange.valueOf(timeRestrictString.uppercase())
                    } else {
                        null
                    }
                    val siteRestrict = parametersJson.optString("site_restrict", "")
                        .takeIf { it.isNotBlank() }
                    val language =
                        LanguageCode.valueOf(parametersJson.optString("language", "EN").uppercase())
                    val country =
                        CountryCode.valueOf(parametersJson.optString("country", "US").uppercase())

                    Log.d(TAG, "Executing Google search for: '$query' (results: $numResults)")

                    searchScope.launch {
                        val packageName = this@GoogleSearchService.packageName
                        val apiKey =
                            settings.getSetting(packageName, SETTING_CATEGORY, SETTING_API_KEY)
                        val searchEngineId = settings.getSetting(
                            packageName,
                            SETTING_CATEGORY,
                            SETTING_SEARCH_ENGINE_ID
                        )

                        if (apiKey == null || searchEngineId == null || apiKey == "YOUR_GOOGLE_API_KEY" || searchEngineId == "YOUR_SEARCH_ENGINE_ID") {
                            Log.e(TAG, "Google API credentials not configured")
                            callback.onError("Google Custom Search API not configured. Please set API key and Search Engine ID.")
                            return@launch
                        }

                        try {
                            val googleSearchClient = GoogleSearchClient(
                                context = this@GoogleSearchService,
                                apiKey = apiKey,
                                searchEngineId = searchEngineId
                            )

                            val response = googleSearchClient.search(
                                query = query,
                                numResults = numResults,
                                language = language,
                                country = country,
                                timeRestrict = timeRestrict,
                                siteRestrict = siteRestrict
                            )

                            if (response != null && searchProcessor.hasValidResults(response)) {
                                val processedResults =
                                    searchProcessor.processResults(response, query)
                                Log.d(
                                    TAG,
                                    "Google search successful: ${response.items?.size ?: 0} results"
                                )
                                callback.onSuccess(processedResults)
                            } else {
                                Log.w(TAG, "No valid results found for query: $query")
                                callback.onError("No results found for '$query'")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Google search failed for query: $query", e)
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
                    "Search Google and return structured results optimized for LLM consumption. Use for current events, news, technical information, and knowledge lookups. Returns JSON with search results including title, URL, snippet, and display link."
                isPriority = true
                parameters = arrayOf(
                    ToolParameter().apply {
                        name = "query"
                        type = "string"
                        description = "The search query to execute on Google"
                        required = true
                        enumValues = emptyArray()
                    },
                    ToolParameter().apply {
                        name = "num_results"
                        type = "integer"
                        description = "Number of search results to return (1-10, default: 10)"
                        required = false
                        enumValues = emptyArray()
                    },
                    ToolParameter().apply {
                        name = "time_restrict"
                        type = "string"
                        description =
                            "Time restriction for results (day=past day, week=past week, month=past month, year=past year). Restrict to day or week when searching for current events/news."
                        required = false
                        enumValues = DateRange.entries.map { it.name }.toTypedArray()
                    },
                    ToolParameter().apply {
                        name = "site_restrict"
                        type = "string"
                        description =
                            "Restrict search to specific site (e.g., 'reddit.com', 'stackoverflow.com')"
                        required = false
                        enumValues = emptyArray()
                    },
                    ToolParameter().apply {
                        name = "language"
                        type = "string"
                        description = "Language code for search results (default: 'EN')"
                        required = false
                        enumValues = LanguageCode.entries.map { it.name }.toTypedArray()
                    },
                    ToolParameter().apply {
                        name = "country"
                        type = "string"
                        description = "Country code for geolocation (default: 'US')"
                        required = false
                        enumValues = CountryCode.entries.map { it.name }.toTypedArray()
                    }
                )
            }
        )
    }
}