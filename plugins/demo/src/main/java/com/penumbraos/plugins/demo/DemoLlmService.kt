package com.penumbraos.plugins.demo

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.penumbraos.mabl.sdk.ILlmCallback
import com.penumbraos.mabl.sdk.ILlmService
import com.penumbraos.mabl.sdk.LlmResponse
import com.penumbraos.mabl.sdk.ToolCall

class DemoLlmService : Service() {
    private val TAG = "DemoLlmService"

    private val binder = object : ILlmService.Stub() {
        override fun generateResponse(prompt: String, callback: ILlmCallback) {
            Log.d(TAG, "Received prompt: $prompt")
            
            val response = when {
                prompt.contains("timer", ignoreCase = true) -> {
                    val toolCall = ToolCall().apply {
                        name = "create_timer"
                        parameters = "{\"duration\":\"25 minutes\"}"
                    }
                    LlmResponse().apply {
                        text = "I'll create a timer for 25 minutes."
                        toolCalls = arrayOf(toolCall)
                    }
                }
                else -> {
                    LlmResponse().apply {
                        text = "I received: $prompt. This is a demo LLM response."
                        toolCalls = emptyArray()
                    }
                }
            }
            
            callback.onResponse(response)
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder
}