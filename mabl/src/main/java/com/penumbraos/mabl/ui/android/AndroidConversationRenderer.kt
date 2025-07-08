package com.penumbraos.mabl.ui.android

import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.penumbraos.mabl.ui.interfaces.ConversationRenderer

class AndroidConversationRenderer(
    private val context: Context,
) : ConversationRenderer {

    private val TAG = "AndroidConversationRenderer"

    // Compose state for UI updates
    val conversationState: MutableState<String> = mutableStateOf("")
    val transcriptionState: MutableState<String> = mutableStateOf("")
    val listeningState: MutableState<Boolean> = mutableStateOf(false)
    val errorState: MutableState<String> = mutableStateOf("")

    override fun showMessage(message: String, isUser: Boolean) {
        Log.d(TAG, "Message: $message (isUser: $isUser)")

        val prefix = if (isUser) "You: " else "MABL: "
        conversationState.value += "$prefix$message\n"
    }

    override fun showTranscription(text: String) {
        Log.d(TAG, "Transcription: $text")
        transcriptionState.value = text
    }

    override fun showListening(isListening: Boolean) {
        Log.d(TAG, "Listening: $isListening")
        listeningState.value = isListening

        if (!isListening) {
            transcriptionState.value = ""
        }
    }

    override fun showError(error: String) {
        Log.e(TAG, "Error: $error")
        errorState.value = error
        conversationState.value += "Error: $error\n"
    }

    override fun clearConversation() {
        Log.d(TAG, "Conversation cleared")
        conversationState.value = ""
        transcriptionState.value = ""
        errorState.value = ""
        listeningState.value = false
    }
}