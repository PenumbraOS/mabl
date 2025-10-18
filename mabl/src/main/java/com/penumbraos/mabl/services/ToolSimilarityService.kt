package com.penumbraos.mabl.services

import android.content.Context
import android.util.Log
import com.penumbraos.mabl.sdk.ToolDefinition
import com.penumbraos.mabl.util.SentenceEmbedding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

private const val TAG = "ToolSimilarityService"

data class OfflineIntentClassificationResult(
    val tool: ToolDefinition,
    val similarity: Float,
    val parameters: Map<String, String>
)

class ToolSimilarityService {
    private val sentenceEmbedding = SentenceEmbedding()
    private val toolEmbeddingCache = ConcurrentHashMap<String, FloatArray>()
    private val intentExampleEmbeddingCache = ConcurrentHashMap<String, FloatArray>()
    private var offlineCapableTools: List<ToolDefinition> = emptyList()
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val SIMILARITY_THRESHOLD = 0.5f
        private const val INTENT_THRESHOLD = 0.55f
//        private const val TOOL_CONFIRMATION_MARGIN = 0.05f
    }

    suspend fun initialize(context: Context) {
        withContext(scope.coroutineContext) {
            val modelBytes = ByteArrayOutputStream().use { outputStream ->
                context.assets.open("minilm-l6-v2-qint8-arm64.onnx").copyTo(outputStream)
                outputStream.toByteArray()
            }

            val tokenizerBytes = ByteArrayOutputStream().use { outputStream ->
                context.assets.open("minilm-l6-v2-tokenizer.json").copyTo(outputStream)
                outputStream.toByteArray()
            }

            try {
                sentenceEmbedding.init(
                    modelBytes = modelBytes,
                    tokenizerBytes = tokenizerBytes,
                    useTokenTypeIds = true,
                    outputTensorName = "last_hidden_state",
                    normalizeEmbeddings = true
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to initialize tokenizer: ${e.message}")
                null
            }
        }
    }

    suspend fun precalculateToolEmbeddings(tools: List<ToolDefinition>) {
        withContext(scope.coroutineContext) {
            val offlineCandidates = mutableListOf<ToolDefinition>()
            toolEmbeddingCache.clear()
            intentExampleEmbeddingCache.clear()

            tools.forEach { tool ->
                val toolText = buildToolText(tool)
                toolEmbeddingCache[tool.name] = sentenceEmbedding.encode(toolText)

                if (!tool.examples.isNullOrEmpty()) {
                    offlineCandidates += tool
                }

                tool.examples?.forEachIndexed { index, example ->
                    if (!example.isNullOrBlank()) {
                        val key = intentExampleKey(tool.name, index)
                        intentExampleEmbeddingCache[key] = sentenceEmbedding.encode(example)
                    }
                }
            }
            offlineCapableTools = offlineCandidates
        }
    }

    suspend fun classifyIntent(
        userQuery: String,
    ): OfflineIntentClassificationResult? {
        return withContext(scope.coroutineContext) {
            val queryEmbedding = sentenceEmbedding.encode(userQuery)
            var bestMatch: OfflineIntentClassificationResult? = null

            offlineCapableTools.forEach { tool ->
                val examples = tool.examples ?: emptyArray()
                if (examples.isEmpty()) {
                    return@forEach
                }

                examples.forEachIndexed { index, example ->
                    if (example.isNullOrBlank()) {
                        return@forEachIndexed
                    }

                    val key = intentExampleKey(tool.name, index)
                    val exampleEmbedding = intentExampleEmbeddingCache[key]
                        ?: return@forEachIndexed

                    val score = cosineSimilarity(queryEmbedding, exampleEmbedding)
                    if (score < INTENT_THRESHOLD) {
                        return@forEachIndexed
                    }

//                    val toolEmbedding = toolEmbeddingCache[tool.name]
//                        ?: sentenceEmbedding.encode(buildToolText(tool)).also {
//                            toolEmbeddingCache[tool.name] = it
//                        }
//                    val toolScore = cosineSimilarity(queryEmbedding, toolEmbedding)
//                    Log.e(
//                        "ToolSimilarityService",
//                        "Intent classification result: ${tool.name} $score $toolScore"
//                    )
//
//                    if (toolScore < INTENT_THRESHOLD + TOOL_CONFIRMATION_MARGIN) {
//                        return@forEachIndexed
//                    }

                    if (bestMatch == null || score > bestMatch!!.similarity) {
                        val parameters = extractBooleanParameters(tool, userQuery)
                        bestMatch = OfflineIntentClassificationResult(tool, score, parameters)
                    }
                }
            }

            bestMatch
        }
    }

    suspend fun filterToolsByRelevance(
        tools: List<ToolDefinition>,
        userQuery: String,
        maxTools: Int
    ): List<ToolDefinition> {
        return withContext(scope.coroutineContext) {
            val queryEmbedding = sentenceEmbedding.encode(userQuery)

            val toolScores = tools.map { tool ->
                val toolEmbedding = toolEmbeddingCache[tool.name] ?: run {
                    // Fallback: calculate embedding if not cached
                    val toolText = buildToolText(tool)
                    sentenceEmbedding.encode(toolText)
                }
                val similarity = cosineSimilarity(queryEmbedding, toolEmbedding)

                // Create "Pair"s (tuples)
                tool to similarity
            }

            val scores = toolScores
                .filter { it.second >= SIMILARITY_THRESHOLD }
                .sortedByDescending { it.second }
                .take(maxTools)

            Log.d(TAG, "Filtered tools: ${scores.map { "${it.first.name}: ${it.second}" }}")

            scores.map { it.first }
        }
    }

    private fun buildToolText(tool: ToolDefinition): String {
        val builder = StringBuilder()
        builder.append(tool.name).append(" ")
        builder.append(tool.description).append(" ")

        tool.parameters?.forEach { param ->
            builder.append(param.name).append(" ")
            builder.append(param.description).append(" ")
            builder.append(param.type).append(" ")
        }

        return builder.toString().trim()
    }

    private fun intentExampleKey(toolName: String, index: Int): String = "$toolName$index"

    private fun extractBooleanParameters(
        tool: ToolDefinition,
        userQuery: String
    ): Map<String, String> {
        val params = tool.parameters ?: return emptyMap()
        val normalizedQuery = userQuery.lowercase()

        val results = mutableMapOf<String, String>()
        params.forEach { parameter ->
            if (!parameter.type.equals("boolean", ignoreCase = true)) {
                return@forEach
            }

            val detected = detectBooleanValue(normalizedQuery)
            if (detected != null) {
                results[parameter.name] = if (detected) "true" else "false"
            }
        }

        return results
    }

    private fun detectBooleanValue(normalizedQuery: String): Boolean? {
        val negativeKeywords = listOf(
            "turn off", "disable", "stop", "end", "shut off", "power off", "deactivate", "mute"
        )
        val positiveKeywords = listOf(
            "turn on", "enable", "start", "begin", "power on", "activate", "unmute"
        )

        if (containsAny(normalizedQuery, negativeKeywords)) {
            return false
        }
        if (containsAny(normalizedQuery, positiveKeywords)) {
            return true
        }

        return null
    }

    private fun containsAny(text: String, phrases: List<String>): Boolean {
        return phrases.any { phrase ->
            val trimmed = phrase.trim()
            if (trimmed.contains(" ")) {
                text.contains(trimmed, ignoreCase = true)
            } else {
                text.split(Regex("\\W+")).any { it.equals(trimmed, ignoreCase = true) }
            }
        }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f

        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator == 0f) 0f else dotProduct / denominator
    }

    fun close() {
        sentenceEmbedding.close()
    }
}
