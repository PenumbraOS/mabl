package com.penumbraos.mabl.simulation.services

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import com.penumbraos.mabl.sdk.ISttCallback
import com.penumbraos.mabl.sdk.ISttService
import com.penumbraos.mabl.sdk.MablService
import com.penumbraos.mabl.ui.SimulatorSttRouter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SimulatorSttService : MablService("SimulatorSttService"), SimulatorSttRouter.SttEventHandler {
    private var currentCallback: ISttCallback? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()

        // Register as the STT event handler for simulator controls
        SimulatorSttRouter.instance = this

        // Initialize Android's built-in speech recognizer for live STT mode
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        setupSpeechRecognizer()

        Log.i(TAG, "Simulator STT Service initialized")
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Voice level monitoring - could be used for UI feedback
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Audio buffer - not used in basic implementation
            }

            override fun onEndOfSpeech() {
                Log.d(TAG, "End of speech")
                isListening = false
            }

            override fun onError(error: Int) {
                Log.e(TAG, "Speech recognition error: $error")
                isListening = false
                try {
                    currentCallback?.onError("Speech recognition error: $error")
                } catch (e: RemoteException) {
                    Log.e(TAG, "Callback error", e)
                }
            }

            override fun onResults(results: Bundle?) {
                Log.d(TAG, "Speech recognition results received")
                isListening = false

                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.let { recognizedText ->
                        Log.d(TAG, "Final transcription: $recognizedText")
                        try {
                            currentCallback?.onFinalTranscription(recognizedText)
                        } catch (e: RemoteException) {
                            Log.e(TAG, "Callback error", e)
                        }
                    }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.let { partialText ->
                        Log.d(TAG, "Partial transcription: $partialText")
                        try {
                            currentCallback?.onPartialTranscription(partialText)
                        } catch (e: RemoteException) {
                            Log.e(TAG, "Callback error", e)
                        }
                    }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Additional events - not used in basic implementation
            }
        })
    }

    private val binder = object : ISttService.Stub() {
        override fun startListening(callback: ISttCallback) {
            Log.i(TAG, "STT startListening requested")
            currentCallback = callback
            onSimulatorStartListening()
        }

        override fun stopListening() {
            Log.i(TAG, "STT stopListening requested")
            if (isListening) {
                speechRecognizer?.stopListening()
                isListening = false
            }
        }
    }

    // SimulatorSttRouter.SttEventHandler implementation
    override fun onSimulatorManualInput(text: String) {
        Log.d(TAG, "Manual input received: $text")

        scope.launch {
            try {
                // Simulate the STT callback flow for manual input
                currentCallback?.onPartialTranscription(text)
                currentCallback?.onFinalTranscription(text)
            } catch (e: RemoteException) {
                Log.e(TAG, "Callback error for manual input", e)
            }
        }
    }

    override fun onSimulatorStartListening() {
        Log.d(TAG, "Starting live STT listening")

        // Check for RECORD_AUDIO permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "RECORD_AUDIO permission not granted")
            try {
                currentCallback?.onError("Microphone permission not granted")
            } catch (e: RemoteException) {
                Log.e(TAG, "Callback error", e)
            }
            return
        }

        if (!isListening && speechRecognizer != null) {
            val intent = Intent().apply {
                putExtra(
                    android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            isListening = true
            speechRecognizer?.startListening(intent)
        }
    }

    override fun onSimulatorStopListening() {
        Log.d(TAG, "Stopping live STT listening")

        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
        }
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        SimulatorSttRouter.instance = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    companion object {
        private const val TAG = "SimulatorSttService"
    }
}