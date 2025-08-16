package com.penumbraos.mabl.aipincore

import android.util.Log
import com.penumbraos.mabl.aipincore.view.PlatformViewModel
import com.penumbraos.mabl.ui.interfaces.IPlatformCapabilities

private const val TAG = "SimulatorCapabilities"

class PlatformCapabilities(
    private val platformViewModel: PlatformViewModel
) : IPlatformCapabilities {

    override fun toggleMenu() {
        Log.d(TAG, "Toggling menu visibility")
        platformViewModel.toggleMenuVisible()
    }

    override fun getViewModel(): Any {
        return platformViewModel
    }
}