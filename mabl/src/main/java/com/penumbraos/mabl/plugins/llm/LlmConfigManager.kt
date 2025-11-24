package com.penumbraos.mabl.plugins.llm

import android.annotation.SuppressLint
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

private const val TAG = "LlmConfigService"

@Serializable
data class LlmConfiguration(
    val name: String,
    val apiKey: String,
    val model: String,
    val maxTokens: Int = 1000,
    val temperature: Double = 0.7,
    val systemPrompt: String? = null,
    val baseUrl: String
)

@Serializable
data class LlmConfigFile(
    val configs: List<LlmConfiguration>
)

class LlmConfigManager {

    private var configs: List<LlmConfiguration>? = null
    private val json = Json { ignoreUnknownKeys = true }

    @SuppressLint("SdCardPath")
    private val mablDir = File("/sdcard/penumbra/etc/mabl/")
    private val configFile = File(mablDir, "llm_configs.json")

    fun getAvailableConfigs(): List<LlmConfiguration> {
        Log.d(TAG, "Getting available LLM configurations")

        if (configs == null || configs!!.isEmpty()) {
            configs = loadConfigsFromFile()
        }

        return configs ?: listOf()
    }

    private fun loadConfigsFromFile(): List<LlmConfiguration> {
        return try {
            if (configFile.exists()) {
                Log.d(TAG, "Attempting to load configs")
                val jsonString = configFile.readText()
                val configFile = json.decodeFromString<LlmConfigFile>(jsonString)
                val logMap = configFile.configs.map { config ->
                    """
                        Name: ${config.name}
                        Model: ${config.model}
                        Base URL: ${config.baseUrl}
                        Max Tokens: ${config.maxTokens}
                        Temperature: ${config.temperature}
                    """.trimIndent()
                }
                Log.d(TAG, "Loaded configs from file: ${logMap.joinToString("\n\n")}")
                configFile.configs
            } else {
                Log.e(TAG, "Config file does not exist. Returning empty configs")
                listOf<LlmConfiguration>()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading configs from file. Returning empty configs", e)
            listOf()
        }
    }
}