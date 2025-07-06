package com.penumbraos.plugins.openai

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.aallam.openai.api.model.ModelId

class OpenAiConfig(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "openai_config"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "model"
        private const val KEY_MAX_TOKENS = "max_tokens"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_SYSTEM_PROMPT = "system_prompt"

        // Default values
        private const val DEFAULT_MODEL = "gemini-2.5-flash"
        private const val DEFAULT_MAX_TOKENS = 1000
        private const val DEFAULT_TEMPERATURE = 0.7f
        private const val DEFAULT_SYSTEM_PROMPT =
            "You are a helpful AI assistant. Provide clear, concise, and accurate responses."

        // TODO: Remove
        private const val DEFAULT_API_KEY = "SOMETHING"
    }

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY
        set(value) = prefs.edit { putString(KEY_API_KEY, value) }

    var model: ModelId
        get() = ModelId(prefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL)
        set(value) = prefs.edit { putString(KEY_MODEL, value.id) }

    var maxTokens: Int
        get() = prefs.getInt(KEY_MAX_TOKENS, DEFAULT_MAX_TOKENS)
        set(value) = prefs.edit { putInt(KEY_MAX_TOKENS, value) }

    var temperature: Double
        get() = prefs.getFloat(KEY_TEMPERATURE, DEFAULT_TEMPERATURE).toDouble()
        set(value) = prefs.edit { putFloat(KEY_TEMPERATURE, value.toFloat()) }

    var systemPrompt: String
        get() = prefs.getString(KEY_SYSTEM_PROMPT, DEFAULT_SYSTEM_PROMPT) ?: DEFAULT_SYSTEM_PROMPT
        set(value) = prefs.edit { putString(KEY_SYSTEM_PROMPT, value) }

    fun reset() {
        prefs.edit { clear() }
    }
}