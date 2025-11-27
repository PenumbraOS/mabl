package com.penumbraos.mabl.plugins.llm

import android.annotation.SuppressLint
import android.util.Log
import kotlinx.serialization.json.Json
import java.io.File

private const val TAG = "LlmConfigService"

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
                    val baseUrlInfo = if (config is LlmConfiguration.OpenAI) {
                        "Base URL: ${config.baseUrl}\n                        "
                    } else {
                        ""
                    }
                    """
                        Type: ${config.type}
                        Name: ${config.name}
                        Model: ${config.model}
                        ${baseUrlInfo}Max Tokens: ${config.maxTokens}
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