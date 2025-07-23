package com.penumbraos.mabl.ui

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import androidx.lifecycle.LifecycleCoroutineScope
import com.penumbraos.mabl.aipincore.SettingsStatusBroadcaster
import com.penumbraos.mabl.sdk.ISttCallback
import com.penumbraos.mabl.sdk.ISttService
import com.penumbraos.mabl.ui.interfaces.IConversationRenderer

private const val TAG = "AiPinSimInputHandler"

class InputHandler(
    context: Context,
    statusBroadcaster: SettingsStatusBroadcaster? = null
) : com.penumbraos.mabl.aipincore.InputHandler(context, statusBroadcaster),
    SimulatorEventRouter.TouchpadEventHandler,
    SimulatorSttRouter.SttEventHandler {

    private var sttService: ISttService? = null
    private var sttCallback: ISttCallback? = null
    private var conversationRenderer: IConversationRenderer? = null

    override fun setup(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        sttService: ISttService?,
        sttCallback: ISttCallback,
        conversationRenderer: IConversationRenderer
    ) {
        SimulatorEventRouter.instance = this
        SimulatorSttRouter.instance = this
        super.setup(context, lifecycleScope, sttService, sttCallback, conversationRenderer)
    }

    override fun onSimulatorTouchpadEvent(event: MotionEvent) {
        Log.d(TAG, "Simulator touchpad event received")
        super.touchpadGestureManager.processTouchpadEvent(event)
    }

    override fun onSimulatorManualInput(text: String) {
        Log.d(TAG, "Manual input received: $text")
        sttCallback?.onFinalTranscription(text)
        conversationRenderer?.showListening(false)
    }

    override fun onSimulatorStartListening() {
        Log.d(TAG, "Simulator start listening")
        startListening()
        sttService?.startListening(sttCallback!!)
    }

    override fun onSimulatorStopListening() {
        Log.d(TAG, "Simulator stop listening")
        sttService?.stopListening()
        stopListening()
    }
}