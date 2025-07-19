package com.penumbraos.mabl.sdk

import android.os.Build

/**
 * Utility class for detecting device type and capabilities in MABL ecosystem
 */
object DeviceUtils {

    /**
     * Detects if the current device is a real AI Pin device (not simulator)
     * @return true if running on actual AI Pin hardware, false otherwise
     */
    fun isAiPin(): Boolean {
        return try {
            Build.MANUFACTURER.equals("Humane", ignoreCase = true) ||
                    Build.PRODUCT.contains("humane", ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Detects if the current device is an AI Pin simulator
     * This is a best-effort detection for when simulator-specific behavior is needed
     * @return true if likely running in simulator mode, false otherwise
     */
    fun isSimulator(): Boolean {
        // Taken from Flutter
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("sdk_gphone64_arm64")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
    }

    /**
     * Gets a string description of the detected device type
     * @return "Ai Pin", "Simulator", or "Unknown"
     */
    fun getDeviceTypeDescription(): String {
        return when {
            isAiPin() -> "Ai Pin"
            isSimulator() -> "Simulator"
            else -> "Unknown"
        }
    }
}