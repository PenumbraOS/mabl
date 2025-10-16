@file:OptIn(ExperimentalEncodingApi::class)

package com.penumbraos.plugins.openai

import android.annotation.SuppressLint
import android.content.Intent
import android.os.IBinder
import android.system.Os
import android.system.OsConstants
import android.util.Log
import com.aallam.openai.api.chat.*
import com.aallam.openai.api.core.Parameters
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIHost
import com.penumbraos.mabl.sdk.BinderConversationMessage
import com.penumbraos.mabl.sdk.DeviceUtils
import com.penumbraos.mabl.sdk.ILlmCallback
import com.penumbraos.mabl.sdk.ILlmService
import com.penumbraos.mabl.sdk.LlmResponse
import com.penumbraos.mabl.sdk.MablService
import com.penumbraos.mabl.sdk.ToolCall
import com.penumbraos.mabl.sdk.ToolDefinition
import com.penumbraos.mabl.sdk.ToolParameter
import com.penumbraos.sdk.PenumbraClient
import com.penumbraos.sdk.http.ktor.HttpClientPlugin
import io.ktor.client.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val TAG = "OpenAiLlmService"

@Serializable
data class ParameterSchema(
    val type: String,
    val properties: Map<String, PropertySchema> = emptyMap(),
    val required: List<String> = emptyList()
)

@Serializable
data class PropertySchema(
    val type: String,
    val description: String,
    val enum: List<String>? = null
)

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

class OpenAiLlmService : MablService("OpenAiLlmService") {

    private val llmScope = CoroutineScope(Dispatchers.IO)
    private var openAI: OpenAI? = null
    private val configManager = LlmConfigManager()
    private var currentConfig: LlmConfiguration? = null

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()

        llmScope.launch {
            var client: PenumbraClient? = null
            if (DeviceUtils.isAiPin()) {
                client = PenumbraClient(this@OpenAiLlmService)
                client.waitForBridge()
            }

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

                openAI =
                    OpenAI(
                        token = apiKey,
                        host = OpenAIHost(baseUrl),
                        httpClientConfig = {
                            if (DeviceUtils.isAiPin()) {
                                install(HttpClientPlugin) {
                                    // Should have been initialized at start
                                    penumbraClient = client!!
                                }
                            }
                        }
                    )
                Log.w(
                    TAG,
                    "OpenAI client initialized successfully with model: ${currentConfig!!.model}"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize OpenAI client", e)
            }
        }
    }

    private var availableTools: List<Tool>? = null

    private val binder = object : ILlmService.Stub() {
        override fun setAvailableTools(tools: Array<ToolDefinition>) {
            Log.d(TAG, "Received ${tools.size} tool definitions")
            availableTools = convertToolDefinitionsToOpenAI(tools)
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

            if (openAI == null) {
                Log.e(TAG, "OpenAI client not initialized")
                callback.onError("OpenAI client not initialized. Check API key configuration.")
                return
            }

            llmScope.launch {
                try {
                    val conversationMessages = messages.map { message ->
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
                                    while (imageBytes.read(buffer).also { bytesRead = it } != -1) {
                                        byteArrayOutputStream.write(buffer, 0, bytesRead)
                                    }
                                    val imageUrl =
                                        Base64.Default.encode(byteArrayOutputStream.toByteArray())

                                    ChatMessage(
                                        role = ChatRole.User,
                                        content = listOf(
                                            TextPart(message.content),
                                            ImagePart(url = "data:image/jpeg;base64,$imageUrl")
                                        )
                                    )
                                } else {
                                    ChatMessage(
                                        role = ChatRole.User,
                                        content = message.content
                                    )
                                }
                            }

                            "assistant" -> ChatMessage(
                                role = ChatRole.Assistant,
                                content = message.content,
                                toolCalls = message.toolCalls?.map { toolCall ->
                                    function {
                                        id = ToolId(toolCall.id)
                                        function = FunctionCall(
                                            toolCall.name,
                                            toolCall.parameters
                                        )
                                    }
                                }
                            )

                            "tool" -> ChatMessage(
                                role = ChatRole.Tool,
                                content = message.content,
                                toolCallId = message.toolCallId?.let {
                                    ToolId(
                                        it
                                    )
                                }
                            )

                            else -> ChatMessage(
                                role = ChatRole.User,
                                content = message.content
                            )
                        }
                    }

                    val chatMessages = listOf(
                        ChatMessage(
                            role = ChatRole.System,
                            content = currentConfig!!.systemPrompt
                                ?: DEFAULT_PROMPT.trimMargin()
                        )
                    ) + conversationMessages

                    val chatCompletionRequest = ChatCompletionRequest(
                        model = ModelId(currentConfig!!.model),
                        messages = chatMessages,
                        maxTokens = currentConfig!!.maxTokens,
                        temperature = currentConfig!!.temperature,
                        tools = convertToolDefinitionsToOpenAI(tools)
                    )

                    val responseBuilder = StringBuilder()
                    val toolCalls = mutableListOf<ToolCall>()
                    var messageCount = 0

                    val completions = openAI!!.chatCompletions(chatCompletionRequest)
                    completions.onEach { chunk: ChatCompletionChunk ->
                        Log.d(TAG, "Received chunk: $chunk")
                        messageCount += 1
                        chunk.choices.forEach { choice ->
                            choice.delta?.let { delta ->
                                delta.content?.let { content ->
                                    responseBuilder.append(content)
                                    callback.onPartialResponse(content)
                                }

                                delta.toolCalls?.forEach { toolCall ->
                                    if (toolCall.function != null) {
                                        val convertedToolCall = ToolCall().apply {
                                            id = toolCall.id!!.id
                                            name = toolCall.function!!.name
                                            parameters = toolCall.function!!.arguments
                                            isLLM = true
                                        }
                                        toolCalls.add(convertedToolCall)
                                    }
                                }
                            }
                        }
                    }.collect()

                    // Send final response
                    val response = LlmResponse().apply {
                        text = responseBuilder.toString()
                        this.toolCalls = toolCalls.toTypedArray()
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

    private fun convertToolDefinitionsToOpenAI(toolDefinitions: Array<ToolDefinition>): List<Tool>? {
        if (toolDefinitions.isEmpty()) {
            return null
        }

        return toolDefinitions.map { toolDef ->
            Tool.function(
                name = toolDef.name,
                description = toolDef.description,
                parameters = convertParametersToOpenAI(toolDef.parameters)
            )
        }
    }

    private fun convertParametersToOpenAI(parameters: Array<ToolParameter>): Parameters {
        val properties = parameters.associate { param ->
            param.name to PropertySchema(
                type = param.type,
                description = param.description,
                enum = if (param.enumValues.isNotEmpty()) param.enumValues.toList() else null
            )
        }

        val required = parameters.filter { it.required }.map { it.name }

        val schema = ParameterSchema(
            type = "object",
            properties = properties,
            required = required
        )

        return Parameters.fromJsonString(Json.encodeToString(ParameterSchema.serializer(), schema))
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "OpenAI LLM Service destroyed")
    }
}