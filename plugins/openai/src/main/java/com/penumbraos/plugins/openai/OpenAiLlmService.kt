@file:OptIn(ExperimentalEncodingApi::class)

package com.penumbraos.plugins.openai

import android.annotation.SuppressLint
import android.content.Intent
import android.os.IBinder
import android.system.Os
import android.system.OsConstants
import android.util.Log
import com.penumbraos.mabl.sdk.BinderConversationMessage
import com.penumbraos.mabl.sdk.ILlmCallback
import com.penumbraos.mabl.sdk.ILlmService
import com.penumbraos.mabl.sdk.LlmResponse
import com.penumbraos.mabl.sdk.MablService
import com.penumbraos.mabl.sdk.ToolCall
import com.penumbraos.mabl.sdk.ToolDefinition
import com.penumbraos.mabl.sdk.ToolParameter
import com.penumbraos.sdk.PenumbraClient
import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.data.message.AiMessage.aiMessage
import dev.langchain4j.data.message.ImageContent
import dev.langchain4j.data.message.SystemMessage.systemMessage
import dev.langchain4j.data.message.TextContent
import dev.langchain4j.data.message.ToolExecutionResultMessage.toolExecutionResultMessage
import dev.langchain4j.data.message.UserMessage.userMessage
import dev.langchain4j.kotlin.model.chat.StreamingChatModelReply
import dev.langchain4j.kotlin.model.chat.chatFlow
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.chat.request.ChatRequestParameters
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import dev.langchain4j.model.chat.response.ChatResponse
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val TAG = "Langchain4jLlmService"

private const val DEFAULT_PROMPT =
    """You are the MABL voice assistant. Your response will be spoken aloud to the user, so keep the response short and to the point.
        |Your core responsibilities:
        |1. Understand the user's request thoroughly.
        |2. Identify which of the provided tools can best fulfill the request.
        |3. Execute the tool(s) and provide a concise, accurate response based on the tool's output.
        |4. If a tool is necessary to provide up-to-date or factual information (e.g., current news, real-time data), prioritize its use.
        |5. Do NOT make up information. If a tool is required to get the answer, use it.
        |6. If a query requires knowledge beyond your training data, especially for current events or news, the `web_search` tool is essential.
        |7. Do not declare limitations (e.g., "I can only do X") if other relevant tools are available for the user's query. You have access to *all* provided tools.
        |8. If no adequate tool is available, you are allowed to fall back on your own knowledge, but only when you have a high confidence of the answer."""

class OpenAiLlmService : MablService("Langchain4jLlmService") {

    private val llmScope = CoroutineScope(Dispatchers.IO)
    private var model: StreamingChatModel? = null
    private val configManager = LlmConfigManager()
    private var currentConfig: LlmConfiguration? = null

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()

        llmScope.launch {
            var client = PenumbraClient(this@OpenAiLlmService)
            client.waitForBridge()

            try {
                currentConfig = configManager.getAvailableConfigs().first()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load LLM configuration", e)
            }

            if (currentConfig == null) {
                Log.e(TAG, "No valid LLM configuration found")
                return@launch
            }

            try {
                Log.d(TAG, "About to create OpenAI client")
                val apiKey = currentConfig!!.apiKey
                val baseUrl = currentConfig!!.baseUrl

                model = OpenAiStreamingChatModel.builder()
                    .httpClientBuilder(KtorHttpClientBuilder(llmScope, client))
                    .baseUrl(baseUrl).apiKey(apiKey)
                    .modelName(currentConfig!!.model).temperature(currentConfig!!.temperature)
                    .maxTokens(currentConfig!!.maxTokens).build()

                Log.w(
                    TAG,
                    "OpenAI client initialized successfully with model: ${currentConfig!!.model}"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize OpenAI client", e)
            }
        }
    }

    private val binder = object : ILlmService.Stub() {
        // TODO: Remove
        override fun setAvailableTools(tools: Array<ToolDefinition>) {
            Log.d(TAG, "Received ${tools.size} tool definitions")
        }

        override fun generateResponse(
            messages: Array<BinderConversationMessage>,
            tools: Array<ToolDefinition>,
            callback: ILlmCallback
        ) {
            Log.w(
                TAG,
                "Submitting ${messages.size} conversation messages with ${tools.size} filtered tools. Last message: \"${messages.last().content}\""
            )

            if (model == null) {
                Log.e(TAG, "LLM client not initialized")
                callback.onError("LLM client not initialized. Check API key configuration.")
                return
            }

            llmScope.launch {
                try {
                    val responseBuilder = StringBuilder()
                    val toolCalls = mutableListOf<ToolCall>()

                    val completions = model!!.chatFlow {
                        this.messages += systemMessage(
                            currentConfig!!.systemPrompt
                                ?: DEFAULT_PROMPT.trimMargin()
                        )

                        this.messages += messages.map { message ->
                            when (message.type) {
                                "user" -> {
                                    if (message.imageFile != null) {
                                        val fileDescriptor = message.imageFile.fileDescriptor
                                        // Rewind file descriptor so we can reuse them
                                        // TODO: This somehow needs to live in MABL core
                                        Os.lseek(
                                            fileDescriptor,
                                            0,
                                            OsConstants.SEEK_SET
                                        )
                                        val imageBytes =
                                            FileInputStream(fileDescriptor)
                                        val byteArrayOutputStream = ByteArrayOutputStream()
                                        val buffer = ByteArray(4096)
                                        var bytesRead: Int
                                        while (imageBytes.read(buffer)
                                                .also { bytesRead = it } != -1
                                        ) {
                                            byteArrayOutputStream.write(buffer, 0, bytesRead)
                                        }
                                        val imageUrl =
                                            Base64.Default.encode(byteArrayOutputStream.toByteArray())

                                        userMessage(
                                            TextContent(message.content),
                                            ImageContent(
                                                imageUrl,
                                                "image/jpeg",
                                                ImageContent.DetailLevel.HIGH
                                            )
                                        )
                                    } else {
                                        userMessage(TextContent(message.content))
                                    }
                                }

                                "assistant" -> aiMessage(
                                    message.content,
                                    message.toolCalls.map { toolCall ->
                                        ToolExecutionRequest.builder().id(toolCall.id)
                                            .name(toolCall.name).arguments(toolCall.parameters)
                                            .build()
                                    }
                                )

                                // TODO: This tool name might be wrong/necessary
                                "tool" -> toolExecutionResultMessage(
                                    message.toolCallId,
                                    message.toolCallId,
                                    message.content
                                )

                                else -> userMessage(message.content)
                            }
                        }

                        this.parameters =
                            ChatRequestParameters.builder().toolSpecifications(
                                convertToolDefinitionsToAPI(tools)
                            ).build()
                    }

                    var finalResponse: ChatResponse? = null

                    completions
                        .catch { exception ->
                            Log.e(TAG, "Error making request", exception)
                            val content =
                                "LLM model error: ${exception.message?.removePrefix("Stream error: ")}"
                            responseBuilder.append(content)
                            // TODO: This should be onError
                            callback.onPartialResponse(content)
                        }
                        .onEach { chunk ->
                            when (chunk) {
                                is StreamingChatModelReply.CompleteResponse -> {
                                    finalResponse = chunk.response
                                }

                                is StreamingChatModelReply.PartialResponse -> {
                                    callback.onPartialResponse(chunk.partialResponse)
                                }

                                is StreamingChatModelReply.Error -> {
                                    throw chunk.cause
                                }
                            }
                        }
                        .collect()

                    if (finalResponse == null) {
                        // TODO: This should be onError
                        callback.onCompleteResponse(LlmResponse().apply {
                            text = "LLM model error: Empty response"
                        })
                        return@launch
                    }

                    // Send final response
                    val response = LlmResponse().apply {
                        text = finalResponse.aiMessage().text() ?: ""
                        this.toolCalls =
                            finalResponse.aiMessage().toolExecutionRequests().map { request ->
                                ToolCall().apply {
                                    id = request.id()
                                    name = request.name()
                                    parameters = request.arguments()
                                    isLLM = true
                                }
                            }.toTypedArray()
                    }

                    val flattenedCalls = toolCalls.joinToString {
                        "id: ${it.id}, name: ${it.name}, parameters: ${it.parameters}"
                    }
                    Log.w(
                        TAG,
                        "LLM response received: \"${response.text}\", $flattenedCalls"
                    )
                    callback.onCompleteResponse(response)
                } catch (e: Exception) {
                    Log.e(TAG, "Error generating response", e)
                    callback.onError("Error generating response: ${e.message}")
                }
            }
        }
    }

    private fun convertToolDefinitionsToAPI(toolDefinitions: Array<ToolDefinition>): List<ToolSpecification>? {
        if (toolDefinitions.isEmpty()) {
            return null
        }

        return toolDefinitions.map { toolDef ->
            ToolSpecification.builder().name(toolDef.name).description(toolDef.description)
                .parameters(convertParametersToAPI(toolDef.parameters)).build()
        }
    }

    private fun convertParametersToAPI(parameters: Array<ToolParameter>): JsonObjectSchema {
        val builder = JsonObjectSchema.builder()
        val required = mutableListOf<String>()

        for (parameter in parameters) {
            if (parameter.required) {
                required += parameter.name
            }

            when (parameter.type.lowercase()) {
                "string" -> builder.addStringProperty(parameter.name, parameter.description)
                "number", "float", "double", "int" -> builder.addNumberProperty(
                    parameter.name,
                    parameter.description
                )

                "enum" -> builder.addEnumProperty(
                    parameter.name,
                    parameter.enumValues.toList(),
                    parameter.description
                )
            }
        }

        return builder.required(required).build()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Langchain4j LLM service destroyed")
    }
}