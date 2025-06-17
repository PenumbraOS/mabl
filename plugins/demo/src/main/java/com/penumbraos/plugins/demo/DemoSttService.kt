package com.penumbraos.plugins.demo

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.penumbraos.mabl.sdk.ISttCallback
import com.penumbraos.mabl.sdk.ISttService

class DemoSttService : Service() {
    private val mainThread = Handler(Looper.getMainLooper())

    private var speechRecognizer: SpeechRecognizer? = null
    private var currentCallback: ISttCallback? = null

    private val binder = object : ISttService.Stub() {
        override fun startListening(callback: ISttCallback) {
            currentCallback = callback
            startSpeechRecognition()
        }

        override fun stopListening() {
            Handler(Looper.getMainLooper()).post {
                speechRecognizer?.stopListening()
            }
        }
    }

    // TODO: This doesn't work. It looks like maybe Humane removed the STT service?
    private fun startSpeechRecognition() {
        mainThread.post {
            speechRecognizer =
                SpeechRecognizer.createOnDeviceSpeechRecognizer(this@DemoSttService).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: android.os.Bundle?) {
                            try {
                                currentCallback?.onPartialTranscription("Listening...")
                            } catch (e: RemoteException) {
                                Log.e("DemoSttService", "Callback error", e)
                            }
                        }

                        override fun onBeginningOfSpeech() {}

                        override fun onRmsChanged(rmsdB: Float) {}

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {}

                        override fun onError(error: Int) {
                            try {
                                currentCallback?.onError("Recognition error: $error")
                            } catch (e: RemoteException) {
                                Log.e("DemoSttService", "Callback error", e)
                            }
                        }

                        override fun onResults(results: android.os.Bundle) {
                            results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()?.let {
                                    try {
                                        currentCallback?.onFinalTranscription(it)
                                    } catch (e: RemoteException) {
                                        Log.e("DemoSttService", "Callback error", e)
                                    }
                                }
                        }

                        override fun onPartialResults(partialResults: android.os.Bundle) {
                            partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()?.let {
                                    try {
                                        currentCallback?.onPartialTranscription(it)
                                    } catch (e: RemoteException) {
                                        Log.e("DemoSttService", "Callback error", e)
                                    }
                                }
                        }

                        override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
                    })

                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    }
                    startListening(intent)
                }
        }
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }
}