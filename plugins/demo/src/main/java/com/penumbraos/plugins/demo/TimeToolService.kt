package com.penumbraos.plugins.demo

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.penumbraos.mabl.sdk.IToolCallback
import com.penumbraos.mabl.sdk.IToolService
import com.penumbraos.mabl.sdk.ToolCall
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "TimeToolService"

private const val GET_CURRENT_TIME_TOOL = "get_current_time"

class TimeToolService : Service() {

    private val binder = object : IToolService.Stub() {
        override fun executeTool(call: ToolCall, callback: IToolCallback) {
            Log.d(TAG, "Executing tool: ${call.name} with parameters: ${call.parameters}")

            when (call.name) {
                GET_CURRENT_TIME_TOOL -> {
                    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val currentTime = formatter.format(Date())
                    callback.onSuccess("Current time: $currentTime")
                }

                else -> {
                    callback.onError("Unknown tool: ${call.name}")
                }
            }
        }

        override fun getToolDefinitions(): Array<com.penumbraos.mabl.sdk.ToolDefinition> {
            return arrayOf(
                com.penumbraos.mabl.sdk.ToolDefinition().apply {
                    name = GET_CURRENT_TIME_TOOL
                    description = "Get the current date and time"
                    parameters = emptyArray()
                }
            )
        }
    }

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "TimeToolService",
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TimeToolService")
            .setContentText("Speech recognition service is running")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "time_tool_service_channel"
        private const val NOTIFICATION_ID = 1001
    }
}