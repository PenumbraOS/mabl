package com.penumbraos.mabl.ui

import com.penumbraos.mabl.ui.interfaces.IConversationRenderer
import com.penumbraos.mabl.ui.interfaces.IPlatformInputHandler
import com.penumbraos.mabl.ui.interfaces.IPlatformCapabilities

data class UIComponents(
    val conversationRenderer: IConversationRenderer,
    val platformInputHandler: IPlatformInputHandler,
    val platformCapabilities: IPlatformCapabilities
)