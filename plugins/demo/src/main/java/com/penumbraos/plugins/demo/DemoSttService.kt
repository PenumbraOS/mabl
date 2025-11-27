package com.penumbraos.plugins.demo

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.speech.SpeechRecognizer
import android.util.Log
import com.penumbraos.mabl.sdk.ISttCallback
import com.penumbraos.mabl.sdk.ISttService
import com.penumbraos.mabl.sdk.MablService
import com.penumbraos.sdk.PenumbraClient
import com.penumbraos.sdk.api.types.SttRecognitionListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DemoSttService : MablService("DemoSttService") {
    private var currentCallback: ISttCallback? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private lateinit var client: PenumbraClient

    private var isListening = false

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        client = PenumbraClient(applicationContext)

        // Hack to start STT service in advance of usage
        client.stt.launchListenerProcess(applicationContext)

        scope.launch {
            client.waitForBridge()
            Log.i("DemoSttService", "Bridge start received, setting up STT")

            client.stt.initialize(object : SttRecognitionListener() {
                override fun onError(error: Int) {
                    try {
                        // RecognitionError.ERROR_NO_MATCH
                        if (error == 7) {
                            Log.d("DemoSttService", "No speech recognized")
                            currentCallback?.onFinalTranscription("")
                        } else {
                            currentCallback?.onError("Recognition error: $error")
                        }
                    } catch (e: Exception) {
                        Log.e("DemoSttService", "Callback error", e)
                    }
                }

                override fun onResults(results: Bundle?) {
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.let {
                            try {
                                currentCallback?.onFinalTranscription(it)
                            } catch (e: RemoteException) {
                                Log.e("DemoSttService", "Callback error", e)
                            }
                        }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.let {
                            try {
                                currentCallback?.onPartialTranscription(it)
                            } catch (e: RemoteException) {
                                Log.e("DemoSttService", "Callback error", e)
                            }
                        }
                }

                override fun onEndOfSpeech() {
                    Log.i("DemoSttService", "End of speech. Continuing")
                }
            })
        }
    }

    private val binder = object : ISttService.Stub() {
        override fun startListening(callback: ISttCallback) {
            currentCallback = callback
            this@DemoSttService.startListening()
        }

        override fun stopListening() {
            this@DemoSttService.stopListening()
        }
    }

    fun startListening() {
        if (isListening) {
            Log.w("DemoSttService", "Already listening. Not starting STT")
            return
        }

        Log.i("DemoSttService", "Starting STT")
        isListening = true
        client.stt.startListening()
    }

    fun stopListening() {
        Log.i("DemoSttService", "Stopping STT")
        isListening = false
        client.stt.stopListening()
    }

    override fun onDestroy() {
        client.stt.destroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }
}