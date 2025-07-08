package com.penumbraos.mabl.ui

import android.os.Build
import com.penumbraos.mabl.BuildConfig

class AppDeviceDetector {

    fun detect(): DeviceType {
        return when {
            isAiPinFromBuildConfig() -> DeviceType.AI_PIN
            isAiPinFromSystemProperties() -> DeviceType.AI_PIN
            else -> DeviceType.PHONE
        }
    }

    private fun isAiPinFromBuildConfig(): Boolean {
        return try {
            BuildConfig.IS_AI_PIN
        } catch (e: Exception) {
            false
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