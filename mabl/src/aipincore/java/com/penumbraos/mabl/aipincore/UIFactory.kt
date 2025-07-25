package com.penumbraos.mabl.aipincore

import android.content.Context
import com.penumbraos.mabl.services.AllControllers
import com.penumbraos.mabl.ui.UIComponents
import com.penumbraos.mabl.ui.interfaces.IConversationRenderer
import com.penumbraos.mabl.ui.interfaces.IInputHandler
import kotlinx.coroutines.CoroutineScope

open class UIFactory(
    coroutineScope: CoroutineScope,
    private val context: Context,
    private val controllers: AllControllers,
) {
    private val statusBroadcaster = SettingsStatusBroadcaster(context, coroutineScope)

    fun createConversationRenderer(): IConversationRenderer {
        return ConversationRenderer(context, controllers, statusBroadcaster)
    }

    open fun createInputHandler(): IInputHandler {
        return InputHandler(context, statusBroadcaster)
    }

    // Convenience method to create all UI components at once
    fun createUIComponents(): UIComponents {
        return UIComponents(
            conversationRenderer = createConversationRenderer(),
            inputHandler = createInputHandler(),
        )
    }
}