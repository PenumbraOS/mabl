package com.penumbraos.plugins.demo

import android.annotation.SuppressLint
import android.content.Intent
import android.os.IBinder
import android.os.RemoteException
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.penumbraos.mabl.sdk.ITtsCallback
import com.penumbraos.mabl.sdk.ITtsService
import com.penumbraos.mabl.sdk.MablService
import java.util.Locale
import java.util.Timer
import kotlin.concurrent.timerTask

private const val UTTERANCE_ID = "mabl_demo_utterance"

class DemoTtsService : MablService("DemoTtsService"), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var currentCallback: ITtsCallback? = null

    private var utteranceAccumulator = ""
    private var utteranceTimer: Timer? = null

    private val binder = object : ITtsService.Stub() {
        override fun registerCallback(callback: ITtsCallback) {
            currentCallback = callback
        }

        override fun speakImmediately(text: String) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        }

        override fun speakIncremental(text: String) {
            utteranceAccumulator += text

            if (text.contains(Regex("[.,?!:;()]"))) {
                // Contains punctuation, consider this a pause
                val utterance = utteranceAccumulator.trim()
                if (utterance.isNotEmpty()) {
                    Log.i(
                        "DemoTtsService",
                        "Punctuation detected. Speaking \"$utterance\""
                    )
                    flushAccumulator()
                }
            } else if (utteranceTimer == null) {
                utteranceTimer = Timer()
                utteranceTimer?.schedule(timerTask {
                    val utterance = utteranceAccumulator.trim()
                    if (utterance.isNotEmpty()) {
                        Log.i(
                            "DemoTtsService",
                            "TTS timer triggered. Speaking \"$utterance\""
                        )
                        flushAccumulator()
                    }
                }, 500)
            }
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

    private fun flushAccumulator() {
        utteranceTimer?.cancel()
        utteranceTimer = null
        tts?.speak(utteranceAccumulator.trim(), TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID)
        utteranceAccumulator = ""
    }
}