package com.penumbraos.plugins.openai

import android.annotation.SuppressLint
import android.os.IBinder
import android.util.Log
import com.penumbraos.mabl.sdk.ILlmConfigCallback
import com.penumbraos.mabl.sdk.ILlmConfigService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
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

class LlmConfigService : ILlmConfigService {

    private val configScope = CoroutineScope(Dispatchers.IO)
    private var configs: List<LlmConfiguration>? = null
    private val json = Json { ignoreUnknownKeys = true }

    @SuppressLint("SdCardPath")
    private val mablDir = File("/sdcard/penumbra/etc/mabl/")
    private val configFile = File(mablDir, "llm_configs.json")

    override fun getAvailableConfigs(callback: ILlmConfigCallback) {
        Log.d(TAG, "Getting available LLM configurations")

        configScope.launch {
            try {
                val configs = getConfigs()
                val configNames = configs.map { it.name }.toTypedArray()
                Log.d(
                    TAG,
                    "Found ${configNames.size} configurations: ${configNames.joinToString()}"
                )
                callback.onConfigsLoaded(configNames)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading configurations", e)
                callback.onError("Failed to load configurations: ${e.message}")
            }
        }
    }

    // TODO: Make proper Binder service
    override fun asBinder(): IBinder? {
        return null
    }

    private fun getConfigs(): List<LlmConfiguration> {
        if (configs == null) {
            configs = loadConfigsFromFile()
        }

        return configs ?: listOf()
    }

    private fun loadConfigsFromFile(): List<LlmConfiguration> {
        return try {
            if (configFile.exists()) {
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
                Log.d(TAG, "Loading configs from file: ${logMap.joinToString("\n\n")}")
                configFile.configs
            } else {
                Log.d(TAG, "Config file does not exist. Returning empty configs")
                val defaultConfigs = listOf<LlmConfiguration>()
                saveConfigsToFile(defaultConfigs)
                defaultConfigs
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading configs from file. Returning empty configs", e)
            listOf()
        }
    }

    private fun saveConfigsToFile(configs: List<LlmConfiguration>) {
        try {
            val configFile = LlmConfigFile(configs)
            val jsonString = json.encodeToString(configFile)
            mablDir.mkdirs()
            this.configFile.writeText(jsonString)
            Log.d(TAG, "Configs saved to file: $jsonString")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving configs to file", e)
        }
    }

    fun getConfigByName(name: String): LlmConfiguration? {
        return getConfigs().find { it.name == name }
    }
}