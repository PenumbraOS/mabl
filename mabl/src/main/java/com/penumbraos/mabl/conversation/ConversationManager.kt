package com.penumbraos.mabl.conversation

import android.util.Log
import com.penumbraos.mabl.sdk.ConversationMessage
import com.penumbraos.mabl.sdk.ILlmCallback
import com.penumbraos.mabl.sdk.IToolCallback
import com.penumbraos.mabl.sdk.LlmResponse
import com.penumbraos.mabl.sdk.ToolCall
import com.penumbraos.mabl.sdk.ToolDefinition
import com.penumbraos.mabl.services.AllControllers
import com.penumbraos.mabl.data.ConversationRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private const val TAG = "ConversationManager"

@Serializable
data class SerializableToolCall(
    val id: String,
    val name: String,
    val parameters: String
)

class ConversationManager(
    private val allControllers: AllControllers,
    private val conversationRepository: ConversationRepository? = null
) {
    private val conversationHistory = mutableListOf<ConversationMessage>()
    private val pendingToolCalls = mutableMapOf<String, ToolCall>()
    private val pendingToolResults = mutableMapOf<String, String>()
    private var currentConversationId: String? = null
    private val json = Json { ignoreUnknownKeys = true }
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun setConversationId(conversationId: String) {
        this.currentConversationId = conversationId
    }
    
    suspend fun resumeConversation(conversationId: String) {
        this.currentConversationId = conversationId
        
        if (conversationRepository != null) {
            // Load conversation history from database
            val messages = conversationRepository.getConversationMessages(conversationId)
            conversationHistory.clear()
            
            // Convert database messages to SDK messages
            for (dbMessage in messages) {
                val sdkMessage = ConversationMessage().apply {
                    type = dbMessage.type
                    content = dbMessage.content
                    toolCalls = if (dbMessage.toolCalls != null) {
                        try {
                            val serializableToolCalls = json.decodeFromString<Array<SerializableToolCall>>(dbMessage.toolCalls)
                            serializableToolCalls.map { serializable ->
                                ToolCall().apply {
                                    id = serializable.id
                                    name = serializable.name
                                    parameters = serializable.parameters
                                }
                            }.toTypedArray()
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to parse tool calls: ${e.message}")
                            emptyArray()
                        }
                    } else {
                        emptyArray()
                    }
                    toolCallId = dbMessage.toolCallId
                }
                conversationHistory.add(sdkMessage)
            }
            
            Log.d(TAG, "Resumed conversation $conversationId with ${conversationHistory.size} messages")
        }
    }

    fun processUserMessage(userMessage: String, callback: ConversationCallback) {
        val userMsg = ConversationMessage().apply {
            type = "user"
            content = userMessage
            toolCalls = emptyArray()
            toolCallId = null
        }
        conversationHistory.add(userMsg)
        
        // Persist user message to database
        persistMessage("user", userMessage)

        // Filter tools based on user query before sending to LLM
        val filteredTools = runBlocking {
            try {
                allControllers.toolOrchestrator.getFilteredToolDefinitions(userMessage)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to filter tools for query: ${e.message}")
                allControllers.toolOrchestrator.allTools
            }
        }.toTypedArray()

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
            
            // Persist assistant message with tool calls to database
            val serializableToolCalls = response.toolCalls.map { toolCall ->
                SerializableToolCall(
                    id = toolCall.id,
                    name = toolCall.name,
                    parameters = toolCall.parameters
                )
            }.toTypedArray()
            val toolCallsJson = json.encodeToString(serializableToolCalls)
            persistMessage("assistant", responseText, toolCallsJson)

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
            
            // Persist assistant message to database
            persistMessage("assistant", responseText)
            
            callback.onCompleteResponse(responseText)
        }
    }

    private fun continueConversationWithToolResults(
        filteredTools: Array<ToolDefinition>,
        callback: ConversationCallback
    ) {
        // Add tool results to conversation history
        pendingToolResults.forEach { (callId, result) ->
            val toolCall = pendingToolCalls[callId]
            val toolMsg = ConversationMessage().apply {
                type = "tool"
                content = result
                toolCalls = emptyArray()
                toolCallId = callId
            }
            conversationHistory.add(toolMsg)
            
            // Persist tool result to database
            persistMessage("tool", result, null, callId)
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
                    
                    // Persist final assistant response to database
                    persistMessage("assistant", response.text ?: "")
                    
                    callback.onCompleteResponse(response.text ?: "")
                }

                override fun onError(error: String) {
                    callback.onError(error)
                }
            })
    }


    private fun persistMessage(
        type: String, 
        content: String, 
        toolCalls: String? = null, 
        toolCallId: String? = null
    ) {
        if (conversationRepository != null && currentConversationId != null) {
            coroutineScope.launch {
                try {
                    conversationRepository.addMessage(
                        conversationId = currentConversationId!!,
                        type = type,
                        content = content,
                        toolCalls = toolCalls,
                        toolCallId = toolCallId
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to persist message: ${e.message}")
                }
            }
        }
    }

    interface ConversationCallback {
        fun onPartialResponse(newToken: String)
        fun onCompleteResponse(finalResponse: String)
        fun onError(error: String)
    }
}