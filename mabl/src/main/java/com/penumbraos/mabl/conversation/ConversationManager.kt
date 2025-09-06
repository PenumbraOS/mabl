package com.penumbraos.mabl.conversation

import android.content.Context
import android.os.ParcelFileDescriptor
import android.util.Log
import com.penumbraos.mabl.data.repository.ConversationImageRepository
import com.penumbraos.mabl.data.repository.ConversationRepository
import com.penumbraos.mabl.sdk.BinderConversationMessage
import com.penumbraos.mabl.sdk.ILlmCallback
import com.penumbraos.mabl.sdk.IToolCallback
import com.penumbraos.mabl.sdk.LlmResponse
import com.penumbraos.mabl.sdk.ToolCall
import com.penumbraos.mabl.sdk.ToolDefinition
import com.penumbraos.mabl.services.AllControllers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private const val TAG = "ConversationManager"

private const val CONVERSATION_EXPIRATION_TIME = 60 * 3 * 1000

@Serializable
data class SerializableToolCall(
    val id: String,
    val name: String,
    val parameters: String
)

class ConversationManager(
    private val allControllers: AllControllers,
    private val context: Context,
    private val conversationRepository: ConversationRepository,
    private val conversationImageRepository: ConversationImageRepository
) {
    var currentConversationId: String? = null

    private val conversationHistory = mutableListOf<BinderConversationMessage>()
    private val pendingToolCalls = mutableMapOf<String, ToolCall>()
    private val pendingToolResults = mutableMapOf<String, String>()
    private val json = Json { ignoreUnknownKeys = true }
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private var lastMessageTimestamp: Long = 0

    suspend fun startOrContinueConversationWithMessage(
        userMessage: String,
        imageMessage: ByteArray? = null,
        callback: ConversationCallback
    ) {
        val timestamp = System.currentTimeMillis()

        if (currentConversationId == null || timestamp - lastMessageTimestamp > CONVERSATION_EXPIRATION_TIME) {
            startNewConversation()
        }

        lastMessageTimestamp = timestamp

        // Persist to DB and write image data to file so it can be sent over Binder
        val dbImageFile = persistMessage("user", userMessage, imageMessage)

        val message = BinderConversationMessage().apply {
            type = "user"
            content = userMessage
            imageFile = if (dbImageFile != null) ParcelFileDescriptor.open(
                dbImageFile,
                ParcelFileDescriptor.MODE_READ_ONLY
            ) else null
            toolCalls = emptyArray()
            toolCallId = null
        }
        conversationHistory.add(message)

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
            val message = BinderConversationMessage().apply {
                type = "assistant"
                content = responseText
                toolCalls = response.toolCalls
                toolCallId = null
            }
            conversationHistory.add(message)

            // Persist assistant message with tool calls to database
            val serializableToolCalls = response.toolCalls.map { toolCall ->
                SerializableToolCall(
                    id = toolCall.id,
                    name = toolCall.name,
                    parameters = toolCall.parameters
                )
            }.toTypedArray()
            val toolCallsJson = json.encodeToString(serializableToolCalls)
            persistMessageSync("assistant", responseText, toolCallsJson)

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
            val message = BinderConversationMessage().apply {
                type = "assistant"
                content = responseText
                toolCalls = emptyArray()
                toolCallId = null
            }
            conversationHistory.add(message)

            // Persist assistant message to database
            persistMessageSync("assistant", responseText)

            callback.onCompleteResponse(responseText)
        }
    }

    private fun continueConversationWithToolResults(
        filteredTools: Array<ToolDefinition>,
        callback: ConversationCallback
    ) {
        // Add tool results to conversation history
        pendingToolResults.forEach { (callId, result) ->
            val message = BinderConversationMessage().apply {
                type = "tool"
                content = result
                toolCalls = emptyArray()
                toolCallId = callId
            }
            conversationHistory.add(message)

            // Persist tool result to database
            persistMessageSync("tool", result, null, callId)
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
                    val message = BinderConversationMessage().apply {
                        type = "assistant"
                        content = response.text ?: ""
                        toolCalls = emptyArray()
                        toolCallId = null
                    }
                    conversationHistory.add(message)

                    // Persist final assistant response to database
                    persistMessageSync("assistant", response.text ?: "")

                    callback.onCompleteResponse(response.text ?: "")
                }

                override fun onError(error: String) {
                    callback.onError(error)
                }
            })
    }

    private fun persistMessageSync(
        type: String,
        content: String,
        toolCalls: String? = null,
        toolCallId: String? = null
    ) {
        coroutineScope.launch {
            persistMessage(type, content, null, toolCalls, toolCallId)
        }
    }

    private suspend fun persistMessage(
        type: String,
        content: String,
        imageData: ByteArray? = null,
        toolCalls: String? = null,
        toolCallId: String? = null
    ): File? {
        if (currentConversationId != null) {
            try {
                val dbMessage = conversationRepository.addMessage(
                    conversationId = currentConversationId!!,
                    type = type,
                    content = content,
                    // TODO: These don't seem to be populated correctly
                    toolCalls = toolCalls,
                    toolCallId = toolCallId
                )

                if (imageData != null) {
                    val image = conversationImageRepository.saveImage(
                        dbMessage.id,
                        imageData,
                        "image/png"
                    )
                    return image?.second
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist message: ${e.message}")
            }
        }

        return null
    }

    // Session management methods
    suspend fun startNewConversation(): ConversationManager {
        Log.d(TAG, "Starting new conversation")

        val conversation = conversationRepository.createNewConversation()
        currentConversationId = conversation.id
        conversationHistory.clear()
        pendingToolCalls.clear()
        pendingToolResults.clear()
        lastMessageTimestamp = System.currentTimeMillis()

        // Clear all conversation temp data
        // TODO: Maybe move to subdirectory
        context.cacheDir.deleteRecursively()

        Log.d(TAG, "Created new conversation: ${conversation.id}")
        return this
    }

//    suspend fun resumeConversation(conversationId: String): Boolean {
//        Log.d(TAG, "Resuming conversation: $conversationId")
//
//        val conversation = conversationRepository.getConversation(conversationId)
//        if (conversation == null) {
//            Log.w(TAG, "Conversation not found: $conversationId")
//            return false
//        }
//
//        this.currentConversationId = conversationId
//        conversationHistory.clear()
//        pendingToolCalls.clear()
//        pendingToolResults.clear()
//
//        // Load conversation history from database
//        val messages = conversationRepository.getConversationMessages(conversationId)
//
//        // Convert database messages to SDK messages
//        for (dbMessage in messages) {
//            val sdkMessage = ConversationMessage().apply {
//                type = dbMessage.type
//                content = dbMessage.content
//                toolCalls = if (dbMessage.toolCalls != null) {
//                    try {
//                        val serializableToolCalls =
//                            json.decodeFromString<Array<SerializableToolCall>>(dbMessage.toolCalls)
//                        serializableToolCalls.map { serializable ->
//                            ToolCall().apply {
//                                id = serializable.id
//                                name = serializable.name
//                                parameters = serializable.parameters
//                            }
//                        }.toTypedArray()
//                    } catch (e: Exception) {
//                        Log.w(TAG, "Failed to parse tool calls: ${e.message}")
//                        emptyArray()
//                    }
//                } else {
//                    emptyArray()
//                }
//                toolCallId = dbMessage.toolCallId
//            }
//            conversationHistory.add(sdkMessage)
//        }
//
//        conversationRepository.updateLastActivity(conversationId)
//        lastMessageTimestamp = System.currentTimeMillis()
//
//        Log.d(TAG, "Resumed conversation $conversationId with ${conversationHistory.size} messages")
//        return true
//    }
//
//    suspend fun resumeLastConversation(): Boolean {
//        Log.d(TAG, "Resuming last conversation")
//
//        val lastConversation = conversationRepository.getLastActiveConversation()
//        return if (lastConversation != null) {
//            resumeConversation(lastConversation.id)
//        } else {
//            Log.d(TAG, "No previous conversation found, starting new one")
//            startNewConversation()
//            true
//        }
//    }

    interface ConversationCallback {
        fun onPartialResponse(newToken: String)
        fun onCompleteResponse(finalResponse: String)
        fun onError(error: String)
    }
}