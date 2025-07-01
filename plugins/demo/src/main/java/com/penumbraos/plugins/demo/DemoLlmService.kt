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
import com.penumbraos.mabl.sdk.ToolCall

class DemoLlmService : Service() {
    private val TAG = "DemoLlmService"

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private val binder = object : ILlmService.Stub() {
        override fun generateResponse(prompt: String, callback: ILlmCallback) {
            Log.d(TAG, "Received prompt: $prompt")

            val response = when {
                prompt.contains("timer", ignoreCase = true) -> {
                    val toolCall = ToolCall().apply {
                        name = "create_timer"
                        parameters = "{\"duration\":\"25 minutes\"}"
                    }
                    LlmResponse().apply {
                        text = "I'll create a timer for 25 minutes."
                        toolCalls = arrayOf(toolCall)
                    }
                }

                else -> {
                    LlmResponse().apply {
                        text = "I received: $prompt. This is a demo LLM response."
                        toolCalls = emptyArray()
                    }
                }
            }

            callback.onResponse(response)
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