package com.penumbraos.mabl.plugins.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val DEFAULT_MAX_TOKENS = 1000
private const val DEFAULT_TEMPERATURE = 0.7

interface LlmConfig {
    val type: String

    val name: String
    val apiKey: String
    val model: String
    val maxTokens: Int
    val temperature: Double
    val systemPrompt: String?
}

@Serializable
sealed class LlmConfiguration : LlmConfig {

    @Serializable
    @SerialName("gemini")
    data class Gemini(
        override val type: String = "Gemini",

        override val name: String,
        override val apiKey: String,
        override val model: String,
        override val maxTokens: Int = DEFAULT_MAX_TOKENS,
        override val temperature: Double = DEFAULT_TEMPERATURE,
        override val systemPrompt: String? = null
    ) : LlmConfiguration()

    @Serializable
    @SerialName("openai")
    data class OpenAI(
        override val type: String = "OpenAI",

        override val name: String,
        override val apiKey: String,
        override val model: String,
        val baseUrl: String,
        override val maxTokens: Int = DEFAULT_MAX_TOKENS,
        override val temperature: Double = DEFAULT_TEMPERATURE,
        override val systemPrompt: String? = null
    ) : LlmConfiguration()
}

object LlmConfigurationSerializer :
    JsonTransformingSerializer<LlmConfiguration>(LlmConfiguration.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element is JsonObject && "type" !in element) {
            // If no type field, default to "openai"
            return buildJsonObject {
                put("type", "openai")
                element.forEach { (key, value) ->
                    put(key, value)
                }
            }
        }
        return element
    }
}

@Serializable
data class LlmConfigFile(
    @Serializable(with = LlmConfigurationListSerializer::class)
    val configs: List<LlmConfiguration>
)

object LlmConfigurationListSerializer : JsonTransformingSerializer<List<LlmConfiguration>>(
    kotlinx.serialization.builtins.ListSerializer(LlmConfigurationSerializer)
) {
    override fun transformDeserialize(element: JsonElement): JsonElement = element
}