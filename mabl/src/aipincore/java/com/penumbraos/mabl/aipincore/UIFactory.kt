package com.penumbraos.mabl.aipincore

import android.content.Context
import com.penumbraos.mabl.services.AllControllers
import com.penumbraos.mabl.ui.UIComponents
import com.penumbraos.mabl.ui.interfaces.IConversationRenderer
import com.penumbraos.mabl.ui.interfaces.IPlatformCapabilities
import com.penumbraos.mabl.ui.interfaces.IPlatformInputHandler
import com.penumbraos.sdk.PenumbraClient
import kotlinx.coroutines.CoroutineScope

open class UIFactory(
    private val coroutineScope: CoroutineScope,
    private val context: Context,
    private val controllers: AllControllers,
) {
    private val statusBroadcaster = SettingsStatusBroadcaster(context, coroutineScope)

    private val client = PenumbraClient(context)

    fun createConversationRenderer(): IConversationRenderer {
        return ConversationRenderer(context, controllers, statusBroadcaster)
    }

    open fun createPlatformInputHandler(): IPlatformInputHandler {
        return PlatformInputHandler(statusBroadcaster, controllers.viewModel)
    }

    open fun createPlatformCapabilities(): IPlatformCapabilities {
        return PlatformCapabilities(coroutineScope, controllers, controllers.viewModel, client)
    }

    fun createUIComponents(): UIComponents {
        return UIComponents(
            conversationRenderer = createConversationRenderer(),
            platformInputHandler = createPlatformInputHandler(),
            platformCapabilities = createPlatformCapabilities()
        )
    }
}