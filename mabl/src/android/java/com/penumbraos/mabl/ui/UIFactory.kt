package com.penumbraos.mabl.ui

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.penumbraos.mabl.services.AllControllers
import com.penumbraos.mabl.ui.interfaces.IConversationRenderer
import com.penumbraos.mabl.ui.interfaces.IPlatformInputHandler
import com.penumbraos.mabl.ui.interfaces.IPlatformCapabilities
import kotlinx.coroutines.CoroutineScope

class UIFactory(
    private val coroutineScope: CoroutineScope,
    private val context: Context,
    private val controllers: AllControllers
) {

    fun createConversationRenderer(): IConversationRenderer {
        return ConversationRenderer(context, controllers)
    }

    fun createPlatformInputHandler(): IPlatformInputHandler {
        return AndroidPlatformInputHandler()
    }

    fun createPlatformCapabilities(): IPlatformCapabilities {
        return object : IPlatformCapabilities {}
    }

    fun createUIComponents(): UIComponents {
        return UIComponents(
            conversationRenderer = createConversationRenderer(),
            platformInputHandler = createPlatformInputHandler(),
            platformCapabilities = createPlatformCapabilities()
        )
    }
}

