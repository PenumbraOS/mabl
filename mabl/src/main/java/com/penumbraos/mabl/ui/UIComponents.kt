package com.penumbraos.mabl.ui

import com.penumbraos.mabl.ui.interfaces.IConversationRenderer
import com.penumbraos.mabl.ui.interfaces.IPlatformInputHandler

data class UIComponents(
    val conversationRenderer: IConversationRenderer,
    val platformInputHandler: IPlatformInputHandler,
)