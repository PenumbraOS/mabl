package com.penumbraos.mabl.aipincore

import com.penumbraos.mabl.aipincore.server.HttpServer
import com.penumbraos.mabl.aipincore.view.model.PlatformViewModel
import com.penumbraos.mabl.services.AllControllers
import com.penumbraos.mabl.ui.interfaces.IPlatformCapabilities
import com.penumbraos.sdk.PenumbraClient
import kotlinx.coroutines.CoroutineScope

private const val TAG = "AiPinCapabilities"

class PlatformCapabilities(
    private val coroutineScope: CoroutineScope,
    private val allControllers: AllControllers,
    private val platformViewModel: PlatformViewModel,
    private val client: PenumbraClient
) : IPlatformCapabilities {

    private val httpServer = HttpServer(allControllers, coroutineScope, client)

    override fun getViewModel(): Any {
        return platformViewModel
    }
}