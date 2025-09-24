package com.penumbraos.mabl.aipincore.view.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatRelativeTimestamp(timestampMillis: Long): String {
    val now = System.currentTimeMillis()
    val diffMillis = now - timestampMillis

    val diffMinutes = diffMillis / (1000 * 60)
    val diffHours = diffMillis / (1000 * 60 * 60)
    val diffDays = diffMillis / (1000 * 60 * 60 * 24)

    return when {
        diffMinutes < 1 -> "Just now"

        diffMinutes < 60 -> "${diffMinutes} ${if (diffMinutes == 1L) "Minute" else "Minutes"} ago"

        diffHours < 24 -> "${diffHours} ${if (diffHours == 1L) "Hour" else "Hours"} ago"

        diffDays == 1L -> "Yesterday at ${formatTime(timestampMillis)}"

        else -> {
            val date = Date(timestampMillis)
            val formatter = SimpleDateFormat("M/d/yy", Locale.US)
            "${formatter.format(date)} at ${formatTime(timestampMillis)}"
        }
    }
}

fun formatTime(timestampMillis: Long): String {
    val date = Date(timestampMillis)
    val formatter = SimpleDateFormat("h:mm a", Locale.US)
    return formatter.format(date)
}