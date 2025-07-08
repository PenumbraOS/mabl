package com.penumbraos.mabl.ui.aipin

import android.content.Context
import android.util.Log
import com.penumbraos.mabl.ui.interfaces.NavigationController

class AiPinNavigationController(
    private val context: Context,
) : NavigationController {

    private val TAG = "AiPinNavigationController"
    private var currentScreen = "conversation"

    override fun navigateToConversation() {
        Log.d(TAG, "Navigating to conversation")
        currentScreen = "conversation"
    }

    override fun navigateToPluginDiscovery() {
        Log.d(TAG, "Navigating to plugin discovery")
        currentScreen = "plugins"
    }

    override fun navigateToSettings() {
        Log.d(TAG, "Navigating to settings")
        currentScreen = "settings"
    }

    override fun goBack() {
        Log.d(TAG, "Going back")

        // AI Pin: Simple back navigation - return to conversation
        when (currentScreen) {
            "plugins", "settings" -> {
                navigateToConversation()
            }

            else -> {
                // Already at main screen
            }
        }
    }
}