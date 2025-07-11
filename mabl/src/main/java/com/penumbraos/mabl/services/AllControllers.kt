package com.penumbraos.mabl.services

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CompletableDeferred

private const val TAG = "AllControllers"

// TODO: Find a better name
class AllControllers {
    lateinit var llm: LlmController
    lateinit var stt: SttController
    lateinit var tts: TtsController

    val allLoaded = CompletableDeferred<Unit>()

    private fun checkAllConnected() {
        if (llm.isConnected && stt.isConnected && tts.isConnected) {
            allLoaded.complete(Unit)
        }
    }

    fun initialize() {
        llm = LlmController { checkAllConnected() }
        stt = SttController { checkAllConnected() }
        tts = TtsController { checkAllConnected() }
    }

    suspend fun connectAll(context: Context) {
        // TODO: These packages shouldn't be hardcoded
        llm.connect(context, "com.penumbraos.plugins.openai")
        stt.connect(context, "com.penumbraos.plugins.demo")
        tts.connect(context, "com.penumbraos.plugins.demo")

        allLoaded.await()
        Log.d(TAG, "All services connected")
    }

    fun shutdown(context: Context) {
        llm.shutdown(context)
        stt.shutdown(context)
        tts.shutdown(context)
    }
}