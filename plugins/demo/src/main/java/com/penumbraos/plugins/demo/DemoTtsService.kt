package com.penumbraos.plugins.demo

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteException
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
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

    override fun onCreate() {
        super.onCreate()
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
}