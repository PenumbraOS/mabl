package com.penumbraos.mabl.aipincore

import android.content.Context
import android.util.Log
import android.view.KeyEvent
import androidx.lifecycle.LifecycleCoroutineScope
import com.penumbraos.mabl.aipincore.input.ITouchpadGestureDelegate
import com.penumbraos.mabl.aipincore.input.TouchpadGesture
import com.penumbraos.mabl.aipincore.input.TouchpadGestureKind
import com.penumbraos.mabl.aipincore.input.TouchpadGestureManager
import com.penumbraos.mabl.aipincore.view.model.PlatformViewModel
import com.penumbraos.mabl.interaction.IInteractionFlowManager
import com.penumbraos.mabl.ui.interfaces.IPlatformInputHandler
import com.penumbraos.sdk.PenumbraClient
import com.penumbraos.sdk.api.types.HandGestureReceiver
import kotlinx.coroutines.launch


private const val TAG = "PlatformInputHandler"
private const val HAND_GESTURE_COOLDOWN_MS = 500L

open class PlatformInputHandler(
    private val statusBroadcaster: SettingsStatusBroadcaster,
    private val viewModel: PlatformViewModel
) : IPlatformInputHandler {
    private lateinit var client: PenumbraClient
    internal lateinit var touchpadGestureManager: TouchpadGestureManager

    private lateinit var interactionFlowManager: IInteractionFlowManager

    private var lastHandGestureTime = 0L

    override fun setup(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        interactionFlowManager: IInteractionFlowManager
    ) {
        this.client = PenumbraClient(context)
        this.interactionFlowManager = interactionFlowManager

        lifecycleScope.launch {
            client.waitForBridge()
            client.handGesture.register(object : HandGestureReceiver {
                override fun onHandClose() {
                    handleClosedHandGesture()
                }

                override fun onHandPush() {
                    handleHandToggledMenuLayer()
                }
            })
        }

        this.touchpadGestureManager = TouchpadGestureManager(
            context,
            lifecycleScope,
            client,
            object : ITouchpadGestureDelegate {
                override fun onGesture(gesture: TouchpadGesture) {
                    // TODO: Build proper API for Input Handler to perform standardized triggers
                    if (gesture.kind != TouchpadGestureKind.HOLD_END) {
                        // Any gesture that isn't a release should halt talking
                        interactionFlowManager.cancelCurrentFlow()
                    }

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
                    statusBroadcaster.sendTouchpadTapEvent(eventName, gesture.duration.toInt())
                    Log.w(TAG, "Touchpad gesture: $gesture")
                }
            })
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            36 -> {
                if (checkHandGestureCooldown()) {
                    handleClosedHandGesture()
                }
                true
            }

            54 -> {
                if (checkHandGestureCooldown()) {
                    handleHandToggledMenuLayer()
                }
                true
            }
//            52 -> handleHandToggledMainLayer()
            else -> false
        }
    }

    private fun checkHandGestureCooldown(): Boolean {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastHandGestureTime < HAND_GESTURE_COOLDOWN_MS) {
            return false
        }

        lastHandGestureTime = currentTime
        return true
    }

    private fun handleClosedHandGesture() {
        viewModel.backGesture()
    }

    protected open fun handleHandToggledMenuLayer() {
        viewModel.toggleMenuVisible()
    }
}