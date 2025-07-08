package com.penumbraos.plugins.openai

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aallam.openai.api.chat.*
import com.aallam.openai.api.core.Parameters
import com.aallam.openai.client.OpenAI
import com.penumbraos.mabl.sdk.ILlmCallback
import com.penumbraos.mabl.sdk.ILlmService
import com.penumbraos.mabl.sdk.LlmResponse
import com.penumbraos.mabl.sdk.ToolCall
import com.penumbraos.sdk.PenumbraClient
import com.penumbraos.sdk.http.ktor.HttpClientPlugin
import io.ktor.client.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val TAG = "OpenAiLlmService"

class OpenAiLlmService : Service() {

    private val llmScope = CoroutineScope(Dispatchers.IO)
    private var openAI: OpenAI? = null
    private lateinit var config: OpenAiConfig

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        config = OpenAiConfig(this)
        val client = PenumbraClient(this, allowInitFailure = true)

        llmScope.launch {
            client.waitForBridge()

            try {
                Log.d(TAG, "About to create OpenAI client")
                openAI =
                    OpenAI(
                        token = config.apiKey,
//                        host = OpenAIHost("[SOMEURL]"),
                        httpClientConfig = {
                            install(HttpClientPlugin) {
                                penumbraClient = client
                            }
                        }
                    )
                Log.d(TAG, "OpenAI client initialized successfully with model: ${config.model.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize OpenAI client", e)
            }
        }
    }

    private val binder = object : ILlmService.Stub() {
        override fun generateResponse(prompt: String, callback: ILlmCallback) {
            Log.d(TAG, "Received prompt: $prompt")

            if (openAI == null) {
                Log.e(TAG, "OpenAI client not initialized")
                callback.onError("OpenAI client not initialized. Check API key configuration.")
                return
            }

            llmScope.launch {
                try {
                    val chatMessages = listOf(
                        ChatMessage(
                            role = ChatRole.System,
                            content = config.systemPrompt
                        ),
                        ChatMessage(
                            role = ChatRole.User,
                            content = prompt
                        )
                    )

                    val chatCompletionRequest = ChatCompletionRequest(
                        model = config.model,
                        messages = chatMessages,
                        maxTokens = config.maxTokens,
                        temperature = config.temperature,
                        tools = getAvailableTools()
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

                                // Handle tool calls
                                delta.toolCalls?.forEach { toolCall ->
                                    if (toolCall.function != null) {
                                        val convertedToolCall = ToolCall().apply {
                                            name = toolCall.function!!.name
                                            parameters = toolCall.function!!.arguments
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
                    Log.d(
                        TAG,
                        "Streaming completed, message count: $messageCount, response: $response"
                    )
                    callback.onCompleteResponse(response)
                } catch (e: Exception) {
                    Log.e(TAG, "Error generating response", e)
                    callback.onError("Error generating response: ${e.message}")
                }
            }
        }
    }

    private fun getAvailableTools(): List<Tool>? {
        return listOf(
            Tool.function(
                name = "get_current_weather",
                description = "Get the current weather in a given location",
                parameters = Parameters.fromJsonString(
                    """
                    {
                        "type": "object",
                        "properties": {
                            "location": {
                                "type": "string",
                                "description": "The city and state, e.g. San Francisco, CA"
                            },
                            "unit": {
                                "type": "string",
                                "enum": ["celsius", "fahrenheit"],
                                "description": "The unit of temperature"
                            }
                        },
                        "required": ["location"]
                    }
                """.trimIndent()
                )
            ),
            Tool.function(
                name = "create_timer",
                description = "Create a timer for a specified duration",
                parameters = Parameters.fromJsonString(
                    """
                    {
                        "type": "object",
                        "properties": {
                            "duration": {
                                "type": "string",
                                "description": "The duration of the timer (e.g., '5 minutes', '1 hour')"
                            },
                            "label": {
                                "type": "string",
                                "description": "Optional label for the timer"
                            }
                        },
                        "required": ["duration"]
                    }
                """.trimIndent()
                )
            )
        )
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OpenAI LLM Service",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "OpenAI-powered language model service"
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OpenAI LLM Service")
            .setContentText("OpenAI language model service is running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "OpenAI LLM Service destroyed")
    }

    companion object {
        private const val CHANNEL_ID = "openai_llm_service_channel"
        private const val NOTIFICATION_ID = 1004
    }
}