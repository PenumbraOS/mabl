package com.penumbraos.mabl.ui

import android.content.Context
import com.penumbraos.mabl.sdk.ISttService
import com.penumbraos.mabl.sdk.ITtsService
import com.penumbraos.mabl.ui.aipin.AiPinConversationRenderer
import com.penumbraos.mabl.ui.aipin.AiPinInputHandler
import com.penumbraos.mabl.ui.aipin.AiPinNavigationController
import com.penumbraos.mabl.ui.android.AndroidConversationRenderer
import com.penumbraos.mabl.ui.android.AndroidInputHandler
import com.penumbraos.mabl.ui.android.AndroidNavigationController
import com.penumbraos.mabl.ui.interfaces.ConversationRenderer
import com.penumbraos.mabl.ui.interfaces.InputHandler
import com.penumbraos.mabl.ui.interfaces.NavigationController

class UIFactory(
    private val context: Context,
    private val deviceType: DeviceType,
    private val sttService: ISttService? = null,
    private val ttsService: ITtsService? = null
) {

    fun createConversationRenderer(): ConversationRenderer {
        return when (deviceType) {
            DeviceType.AI_PIN -> AiPinConversationRenderer(context)
            DeviceType.PHONE -> AndroidConversationRenderer(context)
        }
    }

    fun createInputHandler(): InputHandler {
        return when (deviceType) {
            DeviceType.AI_PIN -> AiPinInputHandler(context)
            DeviceType.PHONE -> AndroidInputHandler(context)
        }
    }

    fun createNavigationController(): NavigationController {
        return when (deviceType) {
            DeviceType.AI_PIN -> AiPinNavigationController(context)
            DeviceType.PHONE -> AndroidNavigationController(context)
        }
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

data class UIComponents(
    val conversationRenderer: ConversationRenderer,
    val inputHandler: InputHandler,
    val navigationController: NavigationController
)