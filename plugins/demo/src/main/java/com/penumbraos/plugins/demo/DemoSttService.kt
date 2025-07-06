package com.penumbraos.plugins.demo

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.penumbraos.mabl.sdk.ISttCallback
import com.penumbraos.mabl.sdk.ISttService
import com.penumbraos.sdk.PenumbraClient
import com.penumbraos.sdk.api.types.SttRecognitionListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Timer
import kotlin.concurrent.timerTask

class DemoSttService : Service() {
    private var currentCallback: ISttCallback? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private lateinit var client: PenumbraClient

    private var utteranceTimer: Timer? = null


    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        client = PenumbraClient(applicationContext, true)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "STT Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("STT Service")
            .setContentText("Speech recognition service is running")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "stt_service_channel"
        private const val NOTIFICATION_ID = 1001
    }
}