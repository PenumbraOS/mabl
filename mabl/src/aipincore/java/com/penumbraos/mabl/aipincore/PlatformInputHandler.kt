package com.penumbraos.mabl.aipincore

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
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
                    if (checkHandGestureCooldown()) {
                        handleClosedHandGesture()
                    }
                }

                override fun onHandPush() {
                    if (checkHandGestureCooldown()) {
                        handleHandToggledMenuLayer()
                    }
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
                        interactionFlowManager.finishListening()
                    }

                    when (gesture.kind) {
                        TouchpadGestureKind.DOUBLE_TAP -> {
                            // TODO: Fix double tap with two fingers
//                            if (gesture.fingerCount == 2) {
                            interactionFlowManager.takePicture()
//                            }
                        }

                        TouchpadGestureKind.HOLD_START -> {
                            interactionFlowManager.startListening(requestImage = gesture.fingerCount == 2)
                        }

                        TouchpadGestureKind.HOLD_END -> {
                            if (interactionFlowManager.isFlowActive()) {
                                interactionFlowManager.finishListening()
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

        ContextCompat.registerReceiver(context, object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_SCREEN_OFF) {
                    viewModel.closeMenu()
                }
            }
        }, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private fun checkHandGestureCooldown(): Boolean {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastHandGestureTime < HAND_GESTURE_COOLDOWN_MS) {
            return false
        }

        lastHandGestureTime = currentTime
        return true
    }

    fun handleClosedHandGesture() {
        viewModel.backGesture()
    }

    fun handleHandToggledMenuLayer() {
        viewModel.toggleMenuVisible()
    }
}