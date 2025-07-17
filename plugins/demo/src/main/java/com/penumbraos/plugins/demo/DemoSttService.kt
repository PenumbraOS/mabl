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
import java.util.Timer
import kotlin.concurrent.timerTask

class DemoSttService : MablService("DemoSttService") {
    private var currentCallback: ISttCallback? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private lateinit var client: PenumbraClient

    private var utteranceTimer: Timer? = null


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
                        currentCallback?.onError("Recognition error: $error")
                    } catch (e: RemoteException) {
                        Log.e("DemoSttService", "Callback error", e)
                    }
                }

                override fun onResults(results: Bundle?) {
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.let {
                            try {
                                utteranceTimer?.cancel()
                                utteranceTimer = null
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
                                updateUtteranceCompletionTimer()
                                currentCallback?.onPartialTranscription(it)
                            } catch (e: RemoteException) {
                                Log.e("DemoSttService", "Callback error", e)
                            }
                        }
                }

                override fun onEndOfSpeech() {
                    Log.i("DemoSttService", "End of speech. Stopping STT")
                    client.stt.stopListening()
                }
            })
        }
    }

    private val binder = object : ISttService.Stub() {
        override fun startListening(callback: ISttCallback) {
            currentCallback = callback
            Log.i("DemoSttService", "Starting STT")
            client.stt.startListening()
        }

        override fun stopListening() {
            Log.i("DemoSttService", "Stopping STT")
            client.stt.stopListening()
        }
    }

    override fun onDestroy() {
        client.stt.destroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private fun updateUtteranceCompletionTimer() {
        utteranceTimer?.cancel()
        utteranceTimer = Timer()
        utteranceTimer?.schedule(timerTask {
            Log.i("DemoSttService", "Timing out waiting for more utterances")
            client.stt.stopListening()
        }, 500)
    }
}