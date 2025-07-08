package com.penumbraos.mabl.ui.android

import android.content.Context
import android.util.Log
import com.penumbraos.mabl.sdk.ISttCallback
import com.penumbraos.mabl.ui.interfaces.InputHandler

class AndroidInputHandler(
    private val context: Context,
) : InputHandler {

    private val TAG = "AndroidInputHandler"
    private var voiceCallback: ((String) -> Unit)? = null
    private var textCallback: ((String) -> Unit)? = null
    private var isListening = false

    private val sttCallback = object : ISttCallback.Stub() {
        override fun onPartialTranscription(partialText: String) {
            Log.d(TAG, "Partial transcription: $partialText")
        }

        override fun onFinalTranscription(finalText: String) {
            Log.d(TAG, "Final transcription: $finalText")
            isListening = false
        }

        override fun onError(errorMessage: String) {
            Log.e(TAG, "STT Error: $errorMessage")
            isListening = false
        }
    }

    override fun onVoiceInput(callback: (String) -> Unit) {
        this.voiceCallback = callback
        Log.d(TAG, "Voice input callback registered")
    }

    override fun onTextInput(callback: (String) -> Unit) {
        this.textCallback = callback
        Log.d(TAG, "Text input callback registered")
    }

    override fun startListening() {
        if (!isListening) {
            isListening = true
        }
    }

    override fun stopListening() {
        if (isListening) {
            isListening = false
        }
    }

    fun handleTextInput(text: String) {
        Log.d(TAG, "Text input received: $text")
        textCallback?.invoke(text)
    }

    fun isCurrentlyListening(): Boolean = isListening
}