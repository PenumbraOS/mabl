package com.penumbraos.mabl.aipincore

import android.content.Context
import com.penumbraos.mabl.services.AllControllers
import com.penumbraos.mabl.ui.UIComponents
import com.penumbraos.mabl.ui.interfaces.IConversationRenderer
import com.penumbraos.mabl.ui.interfaces.IPlatformInputHandler
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

    open fun createPlatformInputHandler(): IPlatformInputHandler {
        return TouchpadGestureHandler(context, statusBroadcaster)
    }

    fun createUIComponents(): UIComponents {
        return UIComponents(
            conversationRenderer = createConversationRenderer(),
            platformInputHandler = createPlatformInputHandler(),
        )
    }
}