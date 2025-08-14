package com.penumbraos.plugins.googlesearch

import android.content.Context
import android.util.Log
import com.penumbraos.sdk.PenumbraClient
import com.penumbraos.sdk.http.ktor.HttpClientPlugin
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val TAG = "GoogleSearchClient"

@Serializable
data class GoogleSearchResponse(
    val kind: String = "",
    val queries: GoogleSearchQueries? = null,
    val items: List<GoogleSearchItem>? = null,
    val searchInformation: GoogleSearchInfo? = null
)

@Serializable
data class GoogleSearchQueries(
    val request: List<GoogleSearchRequest>? = null,
    val nextPage: List<GoogleSearchRequest>? = null
)

@Serializable
data class GoogleSearchRequest(
    val title: String = "",
    val totalResults: String = "0",
    val searchTerms: String = "",
    val count: Int = 10,
    val startIndex: Int = 1
)

@Serializable
data class GoogleSearchInfo(
    val searchTime: Double = 0.0,
    val formattedSearchTime: String = "",
    val totalResults: String = "0",
    val formattedTotalResults: String = ""
)

@Serializable
data class GoogleSearchItem(
    val kind: String = "",
    val title: String = "",
    val htmlTitle: String = "",
    val link: String = "",
    val displayLink: String = "",
    val snippet: String = "",
    val htmlSnippet: String = "",
    val cacheId: String = "",
    val formattedUrl: String = "",
    val htmlFormattedUrl: String = "",
    val pageMap: Map<String, List<Map<String, String>>>? = null
)

enum class DateRange(val value: String) {
    TODAY("1d"), WEEK("1w"), MONTH("1m"), YEAR("1y")
}

enum class LanguageCode {
    EN, ES, FR, DE, IT, PT, RU, JA, KO, ZH
}

enum class CountryCode {
    US, UK, CA, AU, DE, FR, JP, BR, IN
}

class GoogleSearchClient(
    context: Context,
    private val apiKey: String,
    private val searchEngineId: String
) {
    companion object {
        private const val BASE_URL = "https://www.googleapis.com/customsearch/v1"
        private const val REQUEST_TIMEOUT_MS = 10000L
        private const val DEFAULT_NUM_RESULTS = 5
    }

    private val penumbra = PenumbraClient(context)

    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                encodeDefaults = false
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            connectTimeoutMillis = 5000
            socketTimeoutMillis = REQUEST_TIMEOUT_MS
        }
        install(UserAgent) {
            agent = "MABL-GoogleSearch/1.0"
        }
        install(HttpClientPlugin) {
            penumbraClient = penumbra
        }
        defaultRequest {
            header("Accept", "application/json")
        }
    }

    suspend fun search(
        query: String,
        numResults: Int = DEFAULT_NUM_RESULTS,
        language: LanguageCode = LanguageCode.EN,
        country: CountryCode = CountryCode.US,
        timeRestrict: DateRange? = null,
        siteRestrict: String? = null
    ): GoogleSearchResponse? {
        if (query.isBlank()) {
            Log.w(TAG, "Empty query provided")
            return null
        }

        if (apiKey.isBlank() || searchEngineId.isBlank()) {
            Log.e(TAG, "API key or Search Engine ID not configured")
            return null
        }

        Log.d(TAG, "Searching Google Custom Search for: '$query'")

        return try {
            val response = withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
                httpClient.get(BASE_URL) {
                    parameter("key", apiKey)
                    parameter("cx", searchEngineId)
                    parameter("q", query.trim())
                    parameter("num", numResults.coerceIn(1, 10))
                    parameter("lr", "lang_${language.name.lowercase()}")
                    parameter("gl", country.name.lowercase())
                    parameter("safe", "medium") // Safe search

                    // Optional parameters
                    timeRestrict?.let { parameter("dateRestrict", it.value) }
                    siteRestrict?.let { parameter("siteSearch", it) }
                }
            }

            if (response != null) {
                val searchResponse = response.body<GoogleSearchResponse>()
                val itemCount = searchResponse.items?.size ?: 0
                val totalResults = searchResponse.searchInformation?.totalResults ?: "0"

                Log.d(
                    TAG,
                    "Search successful: $itemCount items returned, $totalResults total results"
                )
                searchResponse
            } else {
                Log.w(TAG, "Search request timed out")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search failed for query: $query", e)
            null
        }
    }
}