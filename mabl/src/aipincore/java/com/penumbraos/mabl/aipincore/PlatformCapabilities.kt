package com.penumbraos.mabl.aipincore

import com.penumbraos.mabl.aipincore.view.model.PlatformViewModel
import com.penumbraos.mabl.ui.interfaces.IPlatformCapabilities

private const val TAG = "SimulatorCapabilities"

class PlatformCapabilities(
    private val platformViewModel: PlatformViewModel
) : IPlatformCapabilities {

    override fun getViewModel(): Any {
        return platformViewModel
    }
}