package com.penumbraos.mabl.conversation

import android.util.Log
import com.penumbraos.mabl.sdk.ConversationMessage
import com.penumbraos.mabl.sdk.ILlmCallback
import com.penumbraos.mabl.sdk.IToolCallback
import com.penumbraos.mabl.sdk.LlmResponse
import com.penumbraos.mabl.sdk.ToolCall
import com.penumbraos.mabl.sdk.ToolDefinition
import com.penumbraos.mabl.services.AllControllers
import kotlinx.coroutines.runBlocking

private const val TAG = "ConversationManager"

class ConversationManager(
    private val allControllers: AllControllers
) {
    private val conversationHistory = mutableListOf<ConversationMessage>()
    private val pendingToolCalls = mutableMapOf<String, ToolCall>()
    private val pendingToolResults = mutableMapOf<String, String>()

    fun processUserMessage(userMessage: String, callback: ConversationCallback) {
        val userMsg = ConversationMessage().apply {
            type = "user"
            content = userMessage
            toolCalls = emptyArray()
            toolCallId = null
        }
        conversationHistory.add(userMsg)

        // Filter tools based on user query before sending to LLM
        val filteredTools = runBlocking {
            try {
                allControllers.toolOrchestrator.getFilteredToolDefinitions(userMessage)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to filter tools for query: ${e.message}")
                allControllers.toolOrchestrator.getAvailableToolDefinitions()
            }
        }

        Log.d(
            TAG,
            "Sending ${conversationHistory.size} messages with ${filteredTools.size} filtered tools: ${filteredTools.map { it.name }}"
        )

        allControllers.llm.service?.generateResponse(
            conversationHistory.toTypedArray(),
            filteredTools,
            object : ILlmCallback.Stub() {
                override fun onPartialResponse(newToken: String) {
                    callback.onPartialResponse(newToken)
                }

                override fun onCompleteResponse(response: LlmResponse) {
                    handleLlmResponse(response, filteredTools, callback)
                }

                override fun onError(error: String) {
                    callback.onError(error)
                }
            })
    }

    private fun handleLlmResponse(
        response: LlmResponse,
        filteredTools: Array<ToolDefinition>,
        callback: ConversationCallback
    ) {
        val responseText = if (!response.text.isEmpty()) {
            response.text
        } else {
            "EMPTY RESPONSE"
        }

        if (response.toolCalls.isNotEmpty()) {
            Log.d(TAG, "LLM requested ${response.toolCalls.size} tool calls")

            // Add assistant message with tool calls to history
            val assistantMsg = ConversationMessage().apply {
                type = "assistant"
                content = responseText
                toolCalls = response.toolCalls
                toolCallId = null
            }
            conversationHistory.add(assistantMsg)

            // Execute tool calls
            val toolCallsToExecute = response.toolCalls.size
            var completedToolCalls = 0

            response.toolCalls.forEach { toolCall ->
                val callId = toolCall.id
                pendingToolCalls[callId] = toolCall

                allControllers.toolOrchestrator.executeTool(
                    toolCall,
                    object : IToolCallback.Stub() {
                        override fun onSuccess(result: String) {
                            synchronized(pendingToolResults) {
                                pendingToolResults[callId] = result
                                completedToolCalls++

                                if (completedToolCalls == toolCallsToExecute) {
                                    // All tool calls completed, continue conversation
                                    continueConversationWithToolResults(filteredTools, callback)
                                }
                            }
                        }

                        override fun onError(error: String) {
                            synchronized(pendingToolResults) {
                                pendingToolResults[callId] = "Error: $error"
                                completedToolCalls++

                                if (completedToolCalls == toolCallsToExecute) {
                                    continueConversationWithToolResults(filteredTools, callback)
                                }
                            }
                        }
                    })
            }
        } else {
            // No tool calls, this is the final response
            val assistantMsg = ConversationMessage().apply {
                type = "assistant"
                content = responseText
                toolCalls = emptyArray()
                toolCallId = null
            }
            conversationHistory.add(assistantMsg)
            callback.onCompleteResponse(responseText)
        }
    }

    private fun continueConversationWithToolResults(
        filteredTools: Array<ToolDefinition>,
        callback: ConversationCallback
    ) {
        // Add tool results to conversation history
        pendingToolResults.forEach { (callId, result) ->
            // TODO: Handle this
            val toolCall = pendingToolCalls[callId]
            val toolMsg = ConversationMessage().apply {
                type = "tool"
                content = result
                toolCalls = emptyArray()
                toolCallId = callId
            }
            conversationHistory.add(toolMsg)
        }

        // Clear pending calls
        pendingToolCalls.clear()
        pendingToolResults.clear()

        // Generate follow-up response with tool results
        Log.d(TAG, "Sending ${conversationHistory.size} messages with tool results to LLM")

        allControllers.llm.service?.generateResponse(
            conversationHistory.toTypedArray(),
            filteredTools,
            object : ILlmCallback.Stub() {
                override fun onPartialResponse(newToken: String) {
                    callback.onPartialResponse(newToken)
                }

                override fun onCompleteResponse(response: LlmResponse) {
                    // This should be the final response after tool execution
                    val assistantMsg = ConversationMessage().apply {
                        type = "assistant"
                        content = response.text ?: ""
                        toolCalls = emptyArray()
                        toolCallId = null
                    }
                    conversationHistory.add(assistantMsg)
                    callback.onCompleteResponse(response.text ?: "")
                }

                override fun onError(error: String) {
                    callback.onError(error)
                }
            })
    }


    interface ConversationCallback {
        fun onPartialResponse(newToken: String)
        fun onCompleteResponse(finalResponse: String)
        fun onError(error: String)
    }
}