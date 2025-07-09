package com.penumbraos.mabl.ui

import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.penumbraos.mabl.ui.interfaces.INavigationController

enum class AndroidScreen {
    CONVERSATION,
    PLUGIN_DISCOVERY,
    SETTINGS
}

private const val TAG = "AndroidNavigationController"

class NavigationController(
    private val context: Context
) : INavigationController {
    // Compose state for navigation
    val currentScreen: MutableState<AndroidScreen> = mutableStateOf(AndroidScreen.CONVERSATION)

    override fun navigateToConversation() {
        Log.d(TAG, "Navigating to conversation")
        currentScreen.value = AndroidScreen.CONVERSATION
    }

    override fun navigateToPluginDiscovery() {
        Log.d(TAG, "Navigating to plugin discovery")
        currentScreen.value = AndroidScreen.PLUGIN_DISCOVERY
    }

    override fun navigateToSettings() {
        Log.d(TAG, "Navigating to settings")
        currentScreen.value = AndroidScreen.SETTINGS
    }

    override fun goBack() {
        Log.d(TAG, "Going back")

        when (currentScreen.value) {
            AndroidScreen.PLUGIN_DISCOVERY, AndroidScreen.SETTINGS -> {
                navigateToConversation()
            }

            AndroidScreen.CONVERSATION -> {
                Log.d(TAG, "Already at main screen")
            }
        }
    }
}