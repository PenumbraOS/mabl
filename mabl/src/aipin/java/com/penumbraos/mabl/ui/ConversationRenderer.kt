package com.penumbraos.mabl.ui

import android.content.Context
import android.util.Log
import com.penumbraos.mabl.ui.interfaces.IConversationRenderer

private const val TAG = "AiPinConversationRenderer"

class ConversationRenderer(
    private val context: Context,
) : IConversationRenderer {

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