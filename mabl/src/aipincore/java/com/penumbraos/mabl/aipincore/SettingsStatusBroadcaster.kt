package com.penumbraos.mabl.aipincore

import android.content.Context
import android.util.Log
import com.penumbraos.sdk.PenumbraClient
import com.penumbraos.sdk.api.SettingsClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "SettingsStatusBroadcaster"

const val SETTING_APP_ID = "com.penumbraos.mabl"
const val SETTING_DEBUG_CATEGORY = "debug"
const val SETTING_DEBUG_CURSOR = "debugCursor"

class SettingsStatusBroadcaster(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private var settingsClient: SettingsClient? = null

    init {
        initializeSettingsClient()
    }

    private fun initializeSettingsClient() {
        coroutineScope.launch {
            try {
                val client = PenumbraClient(context)
                client.waitForBridge()
                settingsClient = client.settings
                settingsClient?.registerSettings(SETTING_APP_ID) {
                    category(SETTING_DEBUG_CATEGORY) {
                        booleanSetting(SETTING_DEBUG_CURSOR, false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize MABLStatusBroadcaster", e)
            }
        }
    }

    // Conversation Status Updates
    fun sendTranscribingStatus(partialText: String) {
        settingsClient?.sendStatusUpdate(
            SETTING_APP_ID, "conversation", mapOf(
                "state" to "transcribing",
                "partialText" to partialText
            )
        )
    }

    fun sendAIThinkingStatus(userMessage: String) {
        settingsClient?.sendStatusUpdate(
            SETTING_APP_ID, "conversation", mapOf(
                "state" to "aiThinking",
                "userMessage" to userMessage
            )
        )
    }

    fun sendAIRespondingStatus(streamingToken: String) {
        settingsClient?.sendStatusUpdate(
            SETTING_APP_ID, "conversation", mapOf(
                "state" to "aiResponding",
                "streamingToken" to streamingToken
            )
        )
    }

    fun sendIdleStatus(lastResponse: String) {
        settingsClient?.sendStatusUpdate(
            SETTING_APP_ID, "conversation", mapOf(
                "state" to "idle",
                "lastResponse" to lastResponse
            )
        )
    }

    fun sendErrorStatus(errorMessage: String) {
        settingsClient?.sendStatusUpdate(
            SETTING_APP_ID, "conversation", mapOf(
                "state" to "error",
                "errorMessage" to errorMessage
            )
        )
    }

    // Events
    fun sendUserMessageEvent(text: String) {
        settingsClient?.sendEvent(
            SETTING_APP_ID, "userMessage", mapOf(
                "text" to text
            )
        )
    }

    fun sendAIResponseEvent(text: String, hasToolCalls: Boolean) {
        settingsClient?.sendEvent(
            SETTING_APP_ID, "aiResponse", mapOf(
                "text" to text,
                "hasToolCalls" to hasToolCalls
            )
        )
    }

    fun sendTouchpadTapEvent(tapType: String, duration: Int) {
        settingsClient?.sendEvent(
            SETTING_APP_ID, "touchpadTap", mapOf(
                "tapType" to tapType,
                "duration" to duration
            )
        )
    }

    fun sendSTTErrorEvent(error: String, source: String = "unknown") {
        settingsClient?.sendEvent(
            SETTING_APP_ID, "sttError", mapOf(
                "error" to error,
                "source" to source
            )
        )
    }

    fun sendLLMErrorEvent(error: String) {
        settingsClient?.sendEvent(
            SETTING_APP_ID, "llmError", mapOf(
                "error" to error
            )
        )
    }

    fun isInitialized(): Boolean = settingsClient != null
}