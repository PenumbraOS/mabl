package com.penumbraos.mabl.sdk

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import androidx.core.app.NotificationCompat

abstract class MablService(private val name: String) : Service() {
    override fun onCreate() {
        super.onCreate()

        // TODO: Remove this requirement entirely
        createNotificationChannel()
        startForeground(1001, createNotification())
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            name,
            name,
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, name)
            .setContentTitle(name)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }
}