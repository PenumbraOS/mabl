package com.penumbraos.mabl.ui

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.view.KeyEvent
import androidx.lifecycle.LifecycleCoroutineScope
import com.penumbraos.mabl.aipincore.SettingsStatusBroadcaster
import com.penumbraos.mabl.interaction.IInteractionFlowManager

private const val TAG = "SimulatorInputHandler"

class SimulatorInputHandler(
    statusBroadcaster: SettingsStatusBroadcaster? = null,
    private val platformCapabilities: com.penumbraos.mabl.ui.interfaces.IPlatformCapabilities
) : com.penumbraos.mabl.aipincore.PlatformInputHandler(statusBroadcaster),
    SimulatorEventRouter.TouchpadEventHandler,
    SimulatorKeyEventRouter.KeyEventHandler,
    SimulatorSttRouter.SttEventHandler {

    private lateinit var interactionFlowManager: IInteractionFlowManager

    override fun setup(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        interactionFlowManager: IInteractionFlowManager
    ) {
        SimulatorEventRouter.instance = this
        SimulatorKeyEventRouter.instance = this
        SimulatorSttRouter.instance = this
        this.interactionFlowManager = interactionFlowManager
        super.setup(context, lifecycleScope, interactionFlowManager)
    }

    override fun onSimulatorTouchpadEvent(event: MotionEvent) {
        Log.d(TAG, "Simulator touchpad event received")
        super.touchpadGestureManager.processTouchpadEvent(event)
    }

    override fun onSimulatorKeyEvent(keyCode: Int, event: KeyEvent?) {
        Log.d(TAG, "Simulator key event received: keyCode=$keyCode")
        super.onKeyDown(keyCode, event)
    }

    override fun onSimulatorManualInput(text: String) {
        Log.d(TAG, "Manual input received: $text")
        interactionFlowManager.startConversationFromInput(text)
    }

    override fun onSimulatorStartListening() {
        Log.d(TAG, "Simulator start listening")
        interactionFlowManager.startListening()
    }

    override fun onSimulatorStopListening() {
        Log.d(TAG, "Simulator stop listening")
        if (interactionFlowManager.isFlowActive()) {
            interactionFlowManager.cancelCurrentFlow()
        }
    }

    override fun handleHandToggledMenuLayer() {
        super.handleHandToggledMenuLayer()
        platformCapabilities.toggleMenu()
    }
}