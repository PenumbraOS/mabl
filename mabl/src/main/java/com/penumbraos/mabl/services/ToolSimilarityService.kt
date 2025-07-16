package com.penumbraos.mabl.services

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.penumbraos.mabl.sdk.ToolDefinition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.LongBuffer
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

class ToolSimilarityService {
    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private val embeddingCache = ConcurrentHashMap<String, FloatArray>()
    private val toolEmbeddingCache = ConcurrentHashMap<String, FloatArray>()
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val MAX_SEQUENCE_LENGTH = 512
        private const val SIMILARITY_THRESHOLD = 0.5f
    }

    suspend fun initialize(modelBytes: ByteArray) {
        withContext(scope.coroutineContext) {
            ortEnvironment = OrtEnvironment.getEnvironment()
            ortSession = ortEnvironment?.createSession(modelBytes)
        }
    }

    suspend fun precalculateToolEmbeddings(tools: Array<ToolDefinition>) {
        if (ortSession == null) return
        
        withContext(scope.coroutineContext) {
            tools.forEach { tool ->
                val toolText = buildToolText(tool)
                val embedding = getEmbedding(toolText)
                toolEmbeddingCache[tool.name] = embedding
            }
        }
    }

    suspend fun filterToolsByRelevance(
        tools: Array<ToolDefinition>,
        userQuery: String,
        maxTools: Int
    ): Array<ToolDefinition> {
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

            toolScores
                .filter { it.second >= SIMILARITY_THRESHOLD }
                .sortedByDescending { it.second }
                .take(maxTools)
                .map { it.first }
                .toTypedArray()
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