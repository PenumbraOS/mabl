package com.penumbraos.plugins.demo

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.penumbraos.mabl.sdk.ILlmCallback
import com.penumbraos.mabl.sdk.ILlmService
import com.penumbraos.mabl.sdk.LlmResponse
import io.shubham0204.smollm.SmolLM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "DemoLlmService"

class DemoLlmService : Service() {

    private val llmScope = CoroutineScope(Dispatchers.Default)
    private var smolLM: SmolLM? = null

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        llmScope.launch {
            try {
                val llm = SmolLM()
                val modelName = "Qwen3-0.6B-Q4_K_M.gguf"
                Log.d(TAG, "Loading $modelName")
                llm.load("/data/local/tmp/$modelName", SmolLM.InferenceParams(contextSize = 2048))
                Log.d(TAG, "Loaded $modelName")
                smolLM = llm
            } catch (e: Exception) {
                Log.e(TAG, "Error loading LLM", e)
            }
        }
    }

    private val binder = object : ILlmService.Stub() {
        override fun generateResponse(prompt: String, callback: ILlmCallback) {
            Log.d(TAG, "Received prompt: $prompt")

            if (smolLM == null) {
                Log.e(TAG, "LLM not loaded")
                return
            }

            llmScope.launch {
                var tokens = mutableListOf<String>()
                smolLM!!.getResponseAsFlow(prompt).collect { token ->
                    callback.onPartialResponse(token)
                    tokens.add(token)
                }

                val response = LlmResponse().apply {
                    text = tokens.joinToString(" ")
                    toolCalls = emptyArray()
                }
                callback.onCompleteResponse(response)
            }

//            val responseText = smolLM?.getResponse(prompt)
//
//            val response = LlmResponse().apply {
//                text = responseText
//                toolCalls = emptyArray()
//            }
//
////            val response = when {
////                prompt.contains("timer", ignoreCase = true) -> {
////                    val toolCall = ToolCall().apply {
////                        name = "create_timer"
////                        parameters = "{\"duration\":\"25 minutes\"}"
////                    }
////                    LlmResponse().apply {
////                        text = "I'll create a timer for 25 minutes."
////                        toolCalls = arrayOf(toolCall)
////                    }
////                }
////
////                else -> {
////                    LlmResponse().apply {
////                        text = "I received: $prompt. This is a demo LLM response."
////                        toolCalls = emptyArray()
////                    }
////                }
////            }
//
//            callback.onResponse(response)
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "LLM Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LLM Service")
            .setContentText("Language model service is running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "llm_service_channel"
        private const val NOTIFICATION_ID = 1003
    }
}