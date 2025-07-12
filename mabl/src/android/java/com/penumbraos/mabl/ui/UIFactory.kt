package com.penumbraos.mabl.ui

import android.content.Context
import com.penumbraos.mabl.services.AllControllers
import com.penumbraos.mabl.ui.interfaces.IConversationRenderer
import com.penumbraos.mabl.ui.interfaces.IInputHandler
import com.penumbraos.mabl.ui.interfaces.INavigationController

class UIFactory(
    private val context: Context,
    private val controllers: AllControllers
) {

    fun createConversationRenderer(): IConversationRenderer {
        return ConversationRenderer(context, controllers)
    }

    fun createInputHandler(): IInputHandler {
        return InputHandler(context)
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

