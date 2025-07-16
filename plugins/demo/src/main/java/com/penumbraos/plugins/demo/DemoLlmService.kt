package com.penumbraos.plugins.demo

import android.annotation.SuppressLint
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.penumbraos.mabl.sdk.ConversationMessage
import com.penumbraos.mabl.sdk.ILlmCallback
import com.penumbraos.mabl.sdk.ILlmService
import com.penumbraos.mabl.sdk.LlmResponse
import com.penumbraos.mabl.sdk.MablService
import com.penumbraos.mabl.sdk.ToolDefinition
import io.shubham0204.smollm.SmolLM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "DemoLlmService"

class DemoLlmService : MablService("DemoLlmService") {

    private val llmScope = CoroutineScope(Dispatchers.Default)
    private var smolLM: SmolLM? = null

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()

        llmScope.launch {
            try {
                val llm = SmolLM()
                val modelName = "Qwen3-0.6B-Q4_K_M.gguf"
                Log.d(TAG, "Loading $modelName")
                llm.load("/data/local/tmp/$modelName", SmolLM.InferenceParams(contextSize = 2048))
                Log.d(TAG, "Loaded $modelName")
                smolLM = llm
            } catch (e: Exception) {
                Log.e(TAG, "Error loading LLM", e)
            }
        }
    }

    private val binder = object : ILlmService.Stub() {
        override fun setAvailableTools(tools: Array<ToolDefinition>) {
            // Demo LLM service doesn't use tools, so we can ignore this
            Log.d(TAG, "Received ${tools.size} tool definitions (ignored)")
        }

        override fun generateResponse(
            messages: Array<ConversationMessage>,
            tools: Array<ToolDefinition>,
            callback: ILlmCallback
        ) {
            Log.d(TAG, "Received ${messages.size} conversation messages")

            if (smolLM == null) {
                Log.e(TAG, "LLM not loaded")
                callback.onError("LLM not loaded")
                return
            }

            // Convert conversation messages to a simple prompt
            val prompt = messages.joinToString("\n") { message ->
                when (message.type) {
                    "user" -> "User: ${message.content}"
                    "assistant" -> "Assistant: ${message.content}"
                    "tool" -> "Tool Result: ${message.content}"
                    else -> message.content
                }
            }

            llmScope.launch {
                var tokens = mutableListOf<String>()
                smolLM!!.getResponseAsFlow(prompt).collect { token ->
                    callback.onPartialResponse(token)
                    tokens.add(token)
                }

                val response = LlmResponse().apply {
                    text = tokens.joinToString(" ")
                    toolCalls = emptyArray()
                }
                callback.onCompleteResponse(response)
            }

//            val responseText = smolLM?.getResponse(prompt)
//
//            val response = LlmResponse().apply {
//                text = responseText
//                toolCalls = emptyArray()
//            }
//
////            val response = when {
////                prompt.contains("timer", ignoreCase = true) -> {
////                    val toolCall = ToolCall().apply {
////                        name = "create_timer"
////                        parameters = "{\"duration\":\"25 minutes\"}"
////                    }
////                    LlmResponse().apply {
////                        text = "I'll create a timer for 25 minutes."
////                        toolCalls = arrayOf(toolCall)
////                    }
////                }
////
////                else -> {
////                    LlmResponse().apply {
////                        text = "I received: $prompt. This is a demo LLM response."
////                        toolCalls = emptyArray()
////                    }
////                }
////            }
//
//            callback.onResponse(response)
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder
}