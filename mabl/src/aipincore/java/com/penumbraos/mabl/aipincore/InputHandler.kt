package com.penumbraos.mabl.aipincore

import android.content.Context
import android.util.Log
import android.view.InputEvent
import android.view.MotionEvent
import androidx.lifecycle.LifecycleCoroutineScope
import com.penumbraos.mabl.sdk.ISttCallback
import com.penumbraos.mabl.sdk.ISttService
import com.penumbraos.mabl.ui.interfaces.IConversationRenderer
import com.penumbraos.mabl.ui.interfaces.IInputHandler
import com.penumbraos.sdk.PenumbraClient
import com.penumbraos.sdk.api.types.TouchpadInputReceiver
import kotlinx.coroutines.launch

private const val TAG = "AiPinInputHandler"

open class InputHandler(
    context: Context,
    private val statusBroadcaster: MABLStatusBroadcaster? = null
) : IInputHandler {
    private var voiceCallback: ((String) -> Unit)? = null
    private var textCallback: ((String) -> Unit)? = null
    private var isListening = false
    private val client = PenumbraClient(context)

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

        try {
            lifecycleScope.launch {
                client.waitForBridge()

                client.touchpad.register(object : TouchpadInputReceiver {
                    override fun onInputEvent(event: InputEvent) {
                        if (event !is MotionEvent) {
                            return
                        }
                        processTouchpadEvent(event)
                    }
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup touchpad input", e)
        }
    }

    fun processTouchpadEvent(event: MotionEvent) {
        if (event.action == MotionEvent.ACTION_UP &&
            event.eventTime - event.downTime < 200
        ) {
            Log.w(TAG, "Single touchpad tap detected")

            statusBroadcaster?.sendTouchpadTapEvent(
                "single",
                (event.eventTime - event.downTime).toInt()
            )

            conversationRenderer?.showListening(true)
            sttService?.startListening(sttCallback!!)
        }
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