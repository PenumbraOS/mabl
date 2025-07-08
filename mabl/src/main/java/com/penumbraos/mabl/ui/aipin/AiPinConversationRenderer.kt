package com.penumbraos.mabl.ui.aipin

import android.content.Context
import android.util.Log
import com.penumbraos.mabl.ui.interfaces.ConversationRenderer

class AiPinConversationRenderer(
    private val context: Context,
) : ConversationRenderer {

    private val TAG = "AiPinConversationRenderer"

    override fun showMessage(message: String, isUser: Boolean) {
        Log.d(TAG, "Message: $message (isUser: $isUser)")
    }

    override fun showTranscription(text: String) {
        Log.d(TAG, "Transcription: $text")
    }

    override fun showListening(isListening: Boolean) {
        Log.d(TAG, "Listening: $isListening")
    }

    override fun showError(error: String) {
        Log.e(TAG, "Error: $error")
    }

    override fun clearConversation() {
        Log.d(TAG, "Conversation cleared")
    }
}