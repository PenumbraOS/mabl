package com.penumbraos.mabl.aipincore

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.penumbraos.mabl.aipincore.input.ITouchpadGestureDelegate
import com.penumbraos.mabl.aipincore.input.TouchpadGesture
import com.penumbraos.mabl.aipincore.input.TouchpadGestureKind
import com.penumbraos.mabl.aipincore.input.TouchpadGestureManager
import com.penumbraos.mabl.sdk.ISttCallback
import com.penumbraos.mabl.sdk.ISttService
import com.penumbraos.mabl.ui.interfaces.IConversationRenderer
import com.penumbraos.mabl.ui.interfaces.IInputHandler
import com.penumbraos.sdk.PenumbraClient

private const val TAG = "AiPinInputHandler"

open class InputHandler(
    private val context: Context,
    private val statusBroadcaster: SettingsStatusBroadcaster? = null
) : IInputHandler {
    private var voiceCallback: ((String) -> Unit)? = null
    private var textCallback: ((String) -> Unit)? = null
    private var isListening = false
    private val client = PenumbraClient(context)
    internal lateinit var touchpadGestureManager: TouchpadGestureManager

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
        this.sttService = sttService
        this.sttCallback = sttCallback
        this.conversationRenderer = conversationRenderer

        this.touchpadGestureManager = TouchpadGestureManager(
            context,
            lifecycleScope,
            client,
            object : ITouchpadGestureDelegate {
                override fun onGesture(gesture: TouchpadGesture) {
                    // TODO: Build proper API for Input Handler to perform standardized triggers
                    when (gesture.kind) {
                        TouchpadGestureKind.HOLD_START -> {
                            conversationRenderer.showListening(true)
                            sttService?.startListening(sttCallback)
                        }

                        TouchpadGestureKind.HOLD_END -> {
                            conversationRenderer.showListening(false)
                            sttService?.stopListening()
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

    override fun onVoiceInput(callback: (String) -> Unit) {
        this.voiceCallback = callback
    }

    override fun onTextInput(callback: (String) -> Unit) {
        this.textCallback = callback
        // AI Pin: Text input not typically supported, but could use voice-to-text
        Log.d(TAG, "Text input requested - using voice-to-text fallback")
        onVoiceInput(callback)
    }

    override fun startListening() {
        if (!isListening) {
            Log.d(TAG, "Starting voice listening")
            isListening = true
        }
    }

    override fun stopListening() {
        if (isListening) {
            Log.d(TAG, "Stopping voice listening")
            isListening = false
        }
    }
}