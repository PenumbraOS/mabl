package com.penumbraos.mabl.ui

import android.content.Context
import com.penumbraos.mabl.services.AllControllers
import com.penumbraos.mabl.ui.interfaces.IConversationRenderer
import com.penumbraos.mabl.ui.interfaces.IInputHandler
import com.penumbraos.mabl.ui.interfaces.INavigationController
import kotlinx.coroutines.CoroutineScope

class UIFactory(
    coroutineScope: CoroutineScope,
    private val context: Context,
    private val controllers: AllControllers,
) {
    private val statusBroadcaster = MABLStatusBroadcaster(context, coroutineScope)

    fun createConversationRenderer(): IConversationRenderer {
        return ConversationRenderer(context, controllers, statusBroadcaster)
    }

    fun createInputHandler(): IInputHandler {
        return InputHandler(context, statusBroadcaster)
    }

    fun createNavigationController(): INavigationController {
        return NavigationController(context)
    }

    // Convenience method to create all UI components at once
    fun createUIComponents(): UIComponents {
        return UIComponents(
            conversationRenderer = createConversationRenderer(),
            inputHandler = createInputHandler(),
            navigationController = createNavigationController()
        )
    }
}

