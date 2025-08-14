package com.penumbraos.mabl.aipincore

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.penumbraos.mabl.aipincore.input.ITouchpadGestureDelegate
import com.penumbraos.mabl.aipincore.input.TouchpadGesture
import com.penumbraos.mabl.aipincore.input.TouchpadGestureKind
import com.penumbraos.mabl.aipincore.input.TouchpadGestureManager
import com.penumbraos.mabl.interaction.IInteractionFlowManager
import com.penumbraos.mabl.ui.interfaces.IPlatformInputHandler
import com.penumbraos.sdk.PenumbraClient

private const val TAG = "TouchpadGestureHandler"

open class TouchpadGestureHandler(
    private val context: Context,
    private val statusBroadcaster: SettingsStatusBroadcaster? = null
) : IPlatformInputHandler {
    private var isListening = false
    private val client = PenumbraClient(context)
    internal lateinit var touchpadGestureManager: TouchpadGestureManager

    private lateinit var interactionFlowManager: IInteractionFlowManager

    override fun setup(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        interactionFlowManager: IInteractionFlowManager
    ) {
        this.interactionFlowManager = interactionFlowManager

        this.touchpadGestureManager = TouchpadGestureManager(
            context,
            lifecycleScope,
            client,
            object : ITouchpadGestureDelegate {
                override fun onGesture(gesture: TouchpadGesture) {
                    // TODO: Build proper API for Input Handler to perform standardized triggers
                    when (gesture.kind) {
                        TouchpadGestureKind.HOLD_START -> {
                            interactionFlowManager.startListening()
                        }

                        TouchpadGestureKind.HOLD_END -> {
                            if (interactionFlowManager.isFlowActive()) {
                                interactionFlowManager.cancelCurrentFlow()
                            }
                        }

                        else -> {}
                    }

                    val eventName = if (gesture.fingerCount > 1) {
                        "${gesture.kind.name}_MULTI"
                    } else {
                        "${gesture.kind.name}_SINGLE"
                    }
                    statusBroadcaster?.sendTouchpadTapEvent(eventName, gesture.duration.toInt())
                    Log.w(TAG, "Touchpad gesture: $gesture")
                }
            })
    }

    fun startListening() {
        interactionFlowManager.startListening()
    }

    fun stopListening() {
        if (interactionFlowManager.isFlowActive()) {
            interactionFlowManager.cancelCurrentFlow()
        }
    }
}