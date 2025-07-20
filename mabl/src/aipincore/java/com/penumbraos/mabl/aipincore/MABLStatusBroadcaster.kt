package com.penumbraos.mabl.aipincore

import android.content.Context
import android.util.Log
import com.penumbraos.sdk.PenumbraClient
import com.penumbraos.sdk.api.SettingsClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "MABLStatusBroadcaster"

class MABLStatusBroadcaster(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private var settingsClient: SettingsClient? = null
    private val appId = "com.penumbraos.mabl"

    init {
        initializeSettingsClient()
    }

    private fun initializeSettingsClient() {
        coroutineScope.launch {
            try {
                val client = PenumbraClient(context)
                client.waitForBridge()
                settingsClient = client.settings
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize MABLStatusBroadcaster", e)
            }
        }
    }

    // Conversation Status Updates
    fun sendTranscribingStatus(partialText: String) {
        settingsClient?.sendStatusUpdate(
            appId, "conversation", mapOf(
                "state" to "transcribing",
                "partialText" to partialText
            )
        )
    }

    fun sendAIThinkingStatus(userMessage: String) {
        settingsClient?.sendStatusUpdate(
            appId, "conversation", mapOf(
                "state" to "aiThinking",
                "userMessage" to userMessage
            )
        )
    }

    fun sendAIRespondingStatus(streamingToken: String) {
        settingsClient?.sendStatusUpdate(
            appId, "conversation", mapOf(
                "state" to "aiResponding",
                "streamingToken" to streamingToken
            )
        )
    }

    fun sendIdleStatus(lastResponse: String) {
        settingsClient?.sendStatusUpdate(
            appId, "conversation", mapOf(
                "state" to "idle",
                "lastResponse" to lastResponse
            )
        )
    }

    fun sendErrorStatus(errorMessage: String) {
        settingsClient?.sendStatusUpdate(
            appId, "conversation", mapOf(
                "state" to "error",
                "errorMessage" to errorMessage
            )
        )
    }

    // Events
    fun sendUserMessageEvent(text: String) {
        settingsClient?.sendEvent(
            appId, "userMessage", mapOf(
                "text" to text
            )
        )
    }

    fun sendAIResponseEvent(text: String, hasToolCalls: Boolean) {
        settingsClient?.sendEvent(
            appId, "aiResponse", mapOf(
                "text" to text,
                "hasToolCalls" to hasToolCalls
            )
        )
    }

    fun sendTouchpadTapEvent(tapType: String, duration: Int) {
        settingsClient?.sendEvent(
            appId, "touchpadTap", mapOf(
                "tapType" to tapType,
                "duration" to duration
            )
        )
    }

    fun sendSTTErrorEvent(error: String, source: String = "unknown") {
        settingsClient?.sendEvent(
            appId, "sttError", mapOf(
                "error" to error,
                "source" to source
            )
        )
    }

    fun sendLLMErrorEvent(error: String) {
        settingsClient?.sendEvent(
            appId, "llmError", mapOf(
                "error" to error
            )
        )
    }

    fun isInitialized(): Boolean = settingsClient != null
}