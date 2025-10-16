package com.penumbraos.mabl.services

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.util.Log
import com.penumbraos.mabl.sdk.ToolDefinition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.LongBuffer
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

private const val TAG = "ToolSimilarityService"

data class OfflineIntentClassificationResult(
    val tool: ToolDefinition,
    val similarity: Float,
    val parameters: Map<String, String>
)

class ToolSimilarityService {
    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private val embeddingCache = ConcurrentHashMap<String, FloatArray>()
    private val toolEmbeddingCache = ConcurrentHashMap<String, FloatArray>()
    private var offlineCapableTools: List<ToolDefinition> = emptyList()
    private val intentExampleEmbeddingCache = ConcurrentHashMap<String, FloatArray>()
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val MAX_SEQUENCE_LENGTH = 512
        private const val SIMILARITY_THRESHOLD = 0.5f
        private const val INTENT_THRESHOLD = 0.55f
    }

    suspend fun initialize(modelBytes: ByteArray) {
        withContext(scope.coroutineContext) {
            ortEnvironment = OrtEnvironment.getEnvironment()
            ortSession = ortEnvironment?.createSession(modelBytes)
        }
    }

    suspend fun precalculateToolEmbeddings(tools: List<ToolDefinition>) {
        if (ortSession == null) return

        withContext(scope.coroutineContext) {
            tools.forEach { tool ->
                val toolText = buildToolText(tool)
                val embedding = getEmbedding(toolText)
                toolEmbeddingCache[tool.name] = embedding

                if (!tool.examples.isNullOrEmpty()) {
                    offlineCapableTools += tool
                }

                tool.examples?.forEachIndexed { index, example ->
                    if (!example.isNullOrBlank()) {
                        val key = intentExampleKey(tool.name, index)
                        intentExampleEmbeddingCache[key] = getEmbedding(example)
                    }
                }
            }
        }
    }

    suspend fun classifyIntent(
        userQuery: String,
    ): OfflineIntentClassificationResult? {
        if (ortSession == null) {
            Log.w(TAG, "Intent classification requested before model initialization")
            return null
        }

        return withContext(scope.coroutineContext) {
            val queryEmbedding = getEmbedding(userQuery)
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
                    if (exampleEmbedding == null) {
                        return@forEachIndexed
                    }
                    val score = cosineSimilarity(queryEmbedding, exampleEmbedding)

                    if (score >= INTENT_THRESHOLD && (bestMatch == null || score > bestMatch!!.similarity)) {
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
        if (ortSession == null) {
            throw IllegalStateException("Tool similarity service not initialized")
        }

        return withContext(scope.coroutineContext) {
            val queryEmbedding = getEmbedding(userQuery)

            val toolScores = tools.map { tool ->
                val toolEmbedding = toolEmbeddingCache[tool.name] ?: run {
                    // Fallback: calculate embedding if not cached
                    val toolText = buildToolText(tool)
                    getEmbedding(toolText)
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

    private suspend fun getEmbedding(text: String): FloatArray {
        val cacheKey = text.hashCode().toString()
        embeddingCache[cacheKey]?.let { return it }

        return withContext(scope.coroutineContext) {
            val tokenIds = tokenizeText(text)
            val inputIdsTensor = createInputTensor(tokenIds)
            val tokenTypeIdsTensor = createTokenTypeIdsTensor(tokenIds.size)
            val attentionMaskTensor = createAttentionMaskTensor(tokenIds.size)

            val inputs = mapOf(
                "input_ids" to inputIdsTensor,
                "token_type_ids" to tokenTypeIdsTensor,
                "attention_mask" to attentionMaskTensor
            )
            val outputs = ortSession?.run(inputs)

            val embedding = outputs?.get(0)?.value as Array<*>
            val floatEmbedding = (embedding[0] as Array<FloatArray>)[0]

            inputIdsTensor.close()
            tokenTypeIdsTensor.close()
            attentionMaskTensor.close()
            outputs.close()

            embeddingCache[cacheKey] = floatEmbedding
            floatEmbedding
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

    private fun tokenizeText(text: String): IntArray {
        val words = text.lowercase().split(Regex("\\W+"))
        val tokens = mutableListOf<Int>()

        words.forEach { word ->
            if (word.isNotEmpty()) {
                tokens.add(word.hashCode() % 30000)
            }
        }

        return tokens.take(MAX_SEQUENCE_LENGTH).toIntArray()
    }

    private fun createInputTensor(tokenIds: IntArray): OnnxTensor {
        val shape = longArrayOf(1, tokenIds.size.toLong())
        val buffer = LongBuffer.allocate(tokenIds.size)

        tokenIds.forEach { id ->
            buffer.put(id.toLong())
        }
        buffer.flip()

        return OnnxTensor.createTensor(ortEnvironment, buffer, shape)
    }

    private fun createTokenTypeIdsTensor(sequenceLength: Int): OnnxTensor {
        val shape = longArrayOf(1, sequenceLength.toLong())
        val buffer = LongBuffer.allocate(sequenceLength)

        // All tokens are type 0 (single sentence)
        repeat(sequenceLength) {
            buffer.put(0L)
        }
        buffer.flip()

        return OnnxTensor.createTensor(ortEnvironment, buffer, shape)
    }

    private fun createAttentionMaskTensor(sequenceLength: Int): OnnxTensor {
        val shape = longArrayOf(1, sequenceLength.toLong())
        val buffer = LongBuffer.allocate(sequenceLength)

        // All tokens get attention (no padding in our case)
        repeat(sequenceLength) {
            buffer.put(1L)
        }
        buffer.flip()

        return OnnxTensor.createTensor(ortEnvironment, buffer, shape)
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
        ortSession?.close()
        ortEnvironment?.close()
    }
}
