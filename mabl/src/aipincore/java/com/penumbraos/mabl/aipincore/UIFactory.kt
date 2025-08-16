package com.penumbraos.mabl.aipincore

import android.content.Context
import com.penumbraos.mabl.aipincore.view.PlatformViewModel
import com.penumbraos.mabl.services.AllControllers
import com.penumbraos.mabl.ui.UIComponents
import com.penumbraos.mabl.ui.interfaces.IConversationRenderer
import com.penumbraos.mabl.ui.interfaces.IPlatformCapabilities
import com.penumbraos.mabl.ui.interfaces.IPlatformInputHandler
import kotlinx.coroutines.CoroutineScope

open class UIFactory(
    coroutineScope: CoroutineScope,
    private val context: Context,
    private val controllers: AllControllers,
) {
    private val statusBroadcaster = SettingsStatusBroadcaster(context, coroutineScope)
    private val viewModel = PlatformViewModel()

    fun createConversationRenderer(): IConversationRenderer {
        return ConversationRenderer(context, controllers, statusBroadcaster)
    }

    open fun createPlatformInputHandler(): IPlatformInputHandler {
        return PlatformInputHandler(statusBroadcaster)
    }

    open fun createPlatformCapabilities(): IPlatformCapabilities {
        return PlatformCapabilities(viewModel)
    }

    fun createUIComponents(): UIComponents {
        return UIComponents(
            conversationRenderer = createConversationRenderer(),
            platformInputHandler = createPlatformInputHandler(),
            platformCapabilities = createPlatformCapabilities()
        )
    }
}