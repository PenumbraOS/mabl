package com.penumbraos.mabl.ui

import android.content.Context
import android.util.Log
import com.penumbraos.mabl.services.AllControllers
import com.penumbraos.mabl.types.Error
import com.penumbraos.mabl.ui.interfaces.IConversationRenderer

private const val TAG = "AiPinConversationRenderer"

class ConversationRenderer(
    private val context: Context,
    private val controllers: AllControllers,
    private val statusBroadcaster: MABLStatusBroadcaster? = null
) : IConversationRenderer {

    override fun showMessage(message: String, isUser: Boolean) {
        Log.d(TAG, "Message: $message (isUser: $isUser)")
        if (isUser) {
            statusBroadcaster?.sendUserMessageEvent(message)
            statusBroadcaster?.sendAIThinkingStatus(message)
        } else {
            statusBroadcaster?.sendAIResponseEvent(message, false)
            statusBroadcaster?.sendIdleStatus(message)
        }
    }

    override fun showTranscription(text: String) {
        Log.d(TAG, "Transcription: $text")
        statusBroadcaster?.sendTranscribingStatus(text)
    }

    override fun showListening(isListening: Boolean) {
        Log.d(TAG, "Listening: $isListening")
    }

    override fun showError(error: Error) {
        // TODO: Display onscreen
        when (error) {
            is Error.TtsError -> {}
            is Error.SttError -> {
                controllers.tts.service?.speakImmediately("Sorry, I could not hear you")
                statusBroadcaster?.sendSTTErrorEvent(error.message, "conversationRenderer")
            }
            is Error.LlmError -> {
                controllers.tts.service?.speakImmediately("Failed to talk to LLM")
                statusBroadcaster?.sendLLMErrorEvent(error.message)
                statusBroadcaster?.sendErrorStatus(error.message)
            }
        }
    }

    override fun clearConversation() {
        Log.d(TAG, "Conversation cleared")
    }

}