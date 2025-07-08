package com.penumbraos.mabl.ui

import android.content.Context
import android.os.Build

class DeviceDetector(private val context: Context) {
    
    fun detect(): DeviceType {
        return when {
            isAiPinFromSystemProperties() -> DeviceType.AI_PIN
            else -> DeviceType.PHONE
        }
    }
    
    private fun isAiPinFromSystemProperties(): Boolean {
        return try {
            Build.MANUFACTURER.equals("Humane", ignoreCase = true) ||
            Build.PRODUCT.contains("humane", ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }
}