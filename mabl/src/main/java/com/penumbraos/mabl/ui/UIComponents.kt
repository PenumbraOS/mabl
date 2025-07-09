package com.penumbraos.mabl.ui

import com.penumbraos.mabl.ui.interfaces.IConversationRenderer
import com.penumbraos.mabl.ui.interfaces.IInputHandler
import com.penumbraos.mabl.ui.interfaces.INavigationController

data class UIComponents(
    val conversationRenderer: IConversationRenderer,
    val inputHandler: IInputHandler,
    val navigationController: INavigationController
)