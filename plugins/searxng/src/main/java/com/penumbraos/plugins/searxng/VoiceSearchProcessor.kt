package com.penumbraos.plugins.searxng

import android.util.Log
import java.net.URL

private const val TAG = "VoiceSearchProcessor"

class VoiceSearchProcessor {

    companion object {
        private const val MAX_VOICE_RESPONSE_LENGTH = 150
        private const val MAX_SNIPPET_LENGTH = 100
        private val PREFERRED_ENGINES = listOf("wikipedia", "duckduckgo", "bing")
    }

    fun formatForVoice(response: SearxngResponse): String {
        Log.d(TAG, "Processing ${response.results.size} search results for voice response")

        if (response.results.isEmpty()) {
            return "No search results found for '${response.query}'"
        }

        // Check for direct answers first (highest priority)
        response.answers.firstOrNull()?.let { answer ->
            val voiceAnswer = cleanTextForVoice(answer)
            Log.d(TAG, "Using direct answer: $voiceAnswer")
            return voiceAnswer.take(MAX_VOICE_RESPONSE_LENGTH)
        }

        // Check for infobox content (factual information)
        response.infoboxes.firstOrNull()?.let { infobox ->
            if (infobox.content.isNotBlank()) {
                val voiceContent = cleanTextForVoice(infobox.content)
                val source = extractDomain(response.results.firstOrNull()?.url ?: "")
                Log.d(TAG, "Using infobox content from ${infobox.engine}")
                return "${voiceContent.take(MAX_SNIPPET_LENGTH)}. Source: ${infobox.engine}".take(
                    MAX_VOICE_RESPONSE_LENGTH
                )
            }
        }

        // Find best result based on engine preference and content quality
        val bestResult = findBestResult(response.results)

        return formatResultForVoice(bestResult, response.query)
    }

    private fun findBestResult(results: List<SearchResult>): SearchResult {
        // Prioritize by engine preference
        for (preferredEngine in PREFERRED_ENGINES) {
            val result = results.find { result ->
                result.engine == preferredEngine || result.engines.contains(preferredEngine)
            }
            if (result != null && result.content.isNotBlank()) {
                Log.d(TAG, "Selected result from preferred engine: ${result.engine}")
                return result
            }
        }

        // Fallback to first result with decent content
        val resultWithContent = results.find { it.content.length >= 20 }
        if (resultWithContent != null) {
            Log.d(TAG, "Selected result with content from: ${resultWithContent.engine}")
            return resultWithContent
        }

        Log.d(TAG, "Using first available result from: ${results.first().engine}")
        return results.first()
    }

    private fun formatResultForVoice(result: SearchResult, query: String): String {
        val content = if (result.content.isNotBlank()) {
            cleanTextForVoice(result.content)
        } else {
            cleanTextForVoice(result.title)
        }

        val snippet = content.take(MAX_SNIPPET_LENGTH)
        val domain = extractDomain(result.url)
        val engine = if (result.engine.isNotBlank()) result.engine else "web search"

        return "$snippet. Source: $domain via $engine".take(MAX_VOICE_RESPONSE_LENGTH)
    }

    private fun cleanTextForVoice(text: String): String {
        return text
            // Remove HTML tags
            .replace(Regex("<[^>]*>"), "")
            // Remove excessive whitespace
            .replace(Regex("\\s+"), " ")
            // Remove common web artifacts
            // References [1]
            .replace(Regex("\\[\\d+\\]"), "")
            .replace("...", "")
            .replace("Read more", "")
            // Fix common pronunciation issues
            .replace("&amp;", "and")
            .replace("&lt;", "less than")
            .replace("&gt;", "greater than")
            .replace("&quot;", "")
            .replace("&#39;", "'")
            // Remove parenthetical disambiguation
            .replace(Regex("\\([^)]*\\)$"), "")
            .trim()
    }

    private fun extractDomain(url: String): String {
        return try {
            if (url.isBlank()) return "unknown"

            val cleanUrl = if (!url.startsWith("http")) "https://$url" else url
            val domain = URL(cleanUrl).host.lowercase()

            // Simplify common domains for voice
            when {
                domain.contains("wikipedia") -> "Wikipedia"
                domain.contains("stackoverflow") -> "Stack Overflow"
                domain.contains("github") -> "GitHub"
                domain.contains("reddit") -> "Reddit"
                domain.contains("youtube") -> "YouTube"
                else -> domain.removePrefix("www.").split(".").firstOrNull() ?: domain
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract domain from: $url", e)
            "web"
        }
    }

    fun formatSuggestions(response: SearxngResponse): String? {
        if (response.suggestions.isEmpty()) return null

        val suggestion = response.suggestions.first()
        return "Did you mean: $suggestion?"
    }

    fun formatCorrection(response: SearxngResponse): String? {
        if (response.corrections.isEmpty()) return null

        val correction = response.corrections.first()
        return "Showing results for: $correction"
    }

    fun hasValidResults(response: SearxngResponse): Boolean {
        return response.results.isNotEmpty() ||
                response.answers.isNotEmpty() ||
                response.infoboxes.any { it.content.isNotBlank() }
    }
}