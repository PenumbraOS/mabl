package com.penumbraos.mabl.ui.aipin

import android.content.Context
import android.util.Log
import com.penumbraos.mabl.sdk.ISttCallback
import com.penumbraos.mabl.ui.interfaces.InputHandler

class AiPinInputHandler(
    private val context: Context,
) : InputHandler {

    private val TAG = "AiPinInputHandler"
    private var voiceCallback: ((String) -> Unit)? = null
    private var textCallback: ((String) -> Unit)? = null
    private var isListening = false

    private val sttCallback = object : ISttCallback.Stub() {
        override fun onPartialTranscription(partialText: String) {
            Log.d(TAG, "Partial transcription: $partialText")
            // AI Pin: Could provide audio feedback for partial transcription
        }

        override fun onFinalTranscription(finalText: String) {
            Log.d(TAG, "Final transcription: $finalText")
            voiceCallback?.invoke(finalText)
            isListening = false
        }

        override fun onError(errorMessage: String) {
            Log.e(TAG, "STT Error: $errorMessage")
            isListening = false
        }
    }

    fun setupTouchpadInput() {
        // Note: This is now handled in MainActivity
        // This method is kept for interface compatibility
        Log.d(TAG, "Touchpad input setup delegated to MainActivity")
    }

    override fun onVoiceInput(callback: (String) -> Unit) {
        this.voiceCallback = callback
        setupTouchpadInput()
    }

    override fun onTextInput(callback: (String) -> Unit) {
        this.textCallback = callback
        // AI Pin: Text input not typically supported, but could use voice-to-text
        Log.d(TAG, "Text input requested - using voice-to-text fallback")
        onVoiceInput(callback)
    }

    override fun startListening() {
        if (!isListening) {
            Log.d(TAG, "Starting voice listening")
            isListening = true
        }
    }

    override fun stopListening() {
        if (isListening) {
            Log.d(TAG, "Stopping voice listening")
            isListening = false
        }
    }
}