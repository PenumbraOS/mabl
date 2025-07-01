package com.penumbraos.plugins.demo

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import com.penumbraos.mabl.sdk.ITtsCallback
import com.penumbraos.mabl.sdk.ITtsService
import java.util.Locale

class DemoTtsService : Service(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var currentCallback: ITtsCallback? = null

    private val binder = object : ITtsService.Stub() {
        override fun speak(text: String, callback: ITtsCallback) {
            currentCallback = callback
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mabl_utterance")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
        } else {
            Log.e("DemoTtsService", "TextToSpeech initialization failed")
        }
    }

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        tts = TextToSpeech(this, this).apply {
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    try {
                        currentCallback?.onSpeechStarted()
                    } catch (e: RemoteException) {
                        Log.e("DemoTtsService", "Callback error", e)
                    }
                }

                override fun onDone(utteranceId: String?) {
                    try {
                        currentCallback?.onSpeechFinished()
                    } catch (e: RemoteException) {
                        Log.e("DemoTtsService", "Callback error", e)
                    }
                }

                override fun onError(utteranceId: String?) {
                    try {
                        currentCallback?.onError("TTS error")
                    } catch (e: RemoteException) {
                        Log.e("DemoTtsService", "Callback error", e)
                    }
                }
            })
        }
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TTS Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TTS Service")
            .setContentText("Text-to-speech service is running")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "tts_service_channel"
        private const val NOTIFICATION_ID = 1002
    }
}