package com.penumbraos.mabl.ui

import android.content.Context
import com.penumbraos.mabl.aipincore.SettingsStatusBroadcaster
import com.penumbraos.mabl.services.AllControllers
import com.penumbraos.mabl.ui.interfaces.IPlatformInputHandler
import kotlinx.coroutines.CoroutineScope

class UIFactory(
    coroutineScope: CoroutineScope,
    private val context: Context,
    controllers: AllControllers,
) : com.penumbraos.mabl.aipincore.UIFactory(coroutineScope, context, controllers) {
    private val statusBroadcaster = SettingsStatusBroadcaster(context, coroutineScope)

    override fun createPlatformInputHandler(): IPlatformInputHandler {
        return SimulatorInputHandler(statusBroadcaster, createPlatformCapabilities())
    }
}