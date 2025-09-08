package com.penumbraos.mabl.ui

import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.penumbraos.mabl.services.AllControllers
import com.penumbraos.mabl.types.Error
import com.penumbraos.mabl.ui.interfaces.IConversationRenderer

private const val TAG = "AndroidConversationRenderer"

class ConversationRenderer(
    private val context: Context,
    private val controllers: AllControllers
) : IConversationRenderer {

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

    override fun showError(error: Error) {
        errorState.value = error.message
        conversationState.value += "Error: ${error.message}\n"
    }
}