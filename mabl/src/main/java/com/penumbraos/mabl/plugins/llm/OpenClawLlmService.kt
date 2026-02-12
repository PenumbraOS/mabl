package com.penumbraos.mabl.plugins.llm

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
import com.penumbraos.mabl.sdk.ToolDefinition
import com.penumbraos.sdk.PenumbraClient
import com.penumbraos.sdk.api.WebSocketClient
import com.penumbraos.sdk.api.WebSocketMessageType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val TAG = "OpenClawLlmService"
private const val CONFIG_PATH = "/sdcard/penumbra/etc/mabl/openclaw.json"
private const val SESSION_KEY = "pin"

@Serializable
data class OpenClawConfig(
    val gatewayUrl: String,
    val token: String,
    val nodeId: String = "aipin"
)

class OpenClawLlmService : MablService("OpenClawLlmService") {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }
    private var ws: WebSocketClient.WebSocket? = null
    private var config: OpenClawConfig? = null
    private var connected = false
    private var reconnectAttempt = 0
    private var destroyed = false
    private val maxReconnectAttempts = 7
    private var gaveUpReconnecting = false
    private var connectDeferred: CompletableDeferred<Boolean>? = null

    // Pending agent runs: clientRunId -> callback
    private val pendingRuns = ConcurrentHashMap<String, ILlmCallback>()

    // Track which clientRunId is active for incoming chat events
    private var activeRunId: String? = null

    private lateinit var client: PenumbraClient

    override fun onCreate() {
        super.onCreate()
        client = PenumbraClient(this@OpenClawLlmService)
        scope.launch {
            val configFile = File(CONFIG_PATH)
            if (!configFile.exists()) {
                Log.e(TAG, "Config file not found at $CONFIG_PATH")
                return@launch
            }
            config = try {
                json.decodeFromString<OpenClawConfig>(configFile.readText())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse config", e)
                return@launch
            }

            client.waitForBridge()

            try {
                connectGateway()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to OpenClaw gateway", e)
            }
        }
    }

    private suspend fun connectGateway() {
        val cfg = config ?: return
        val deferred = CompletableDeferred<Boolean>()
        connectDeferred = deferred

        Log.d(TAG, "Connecting to OpenClaw gateway at ${cfg.gatewayUrl}")

        val socket = client.websocket.connect(cfg.gatewayUrl)

        socket.onMessage { _, data ->
            val text = String(data)
            try {
                handleFrame(JSONObject(text))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to handle frame: ${e.message}")
            }
        }

        socket.onClose {
            Log.w(TAG, "WebSocket closed")
            connected = false
            ws = null
            connectDeferred?.complete(false)
            failAllPendingRuns("Connection lost")
            scheduleReconnect()
        }

        ws = socket

        // TODO: Properly wait for connect.challenge event and include the nonce in
        // the connect request (needed for device-based auth). Currently we send
        // connect immediately because: (1) token-only auth doesn't require the nonce,
        // and (2) the SDK WebSocketClient has a race where onMessage handlers aren't
        // registered until after connect() returns, so the challenge event gets dropped.
        sendConnectRequest()
    }

    private fun sendConnectRequest() {
        val cfg = config ?: return

        send(JSONObject().apply {
            put("type", "req")
            put("id", nextId())
            put("method", "connect")
            put("params", JSONObject().apply {
                put("minProtocol", 3)
                put("maxProtocol", 3)
                put("role", "node")
                put("client", JSONObject().apply {
                    put("id", "node-host")
                    put("mode", "node")
                    put("platform", "android")
                    put("version", "0.1.0")
                    put("displayName", cfg.nodeId)
                })
                put("auth", JSONObject().apply {
                    put("token", cfg.token)
                })
                put("caps", org.json.JSONArray(listOf("chat")))
            })
        })

        Log.d(TAG, "Connect request sent")
    }

    private fun failAllPendingRuns(reason: String) {
        pendingRuns.forEach { (_, callback) ->
            try { callback.onError(reason) } catch (_: Exception) {}
        }
        pendingRuns.clear()
        activeRunId = null
    }

    private fun scheduleReconnect() {
        if (destroyed) return
        if (reconnectAttempt >= maxReconnectAttempts) {
            Log.w(TAG, "Max reconnect attempts reached, giving up. Will retry on next user request.")
            gaveUpReconnecting = true
            return
        }
        val delayMs = 1000L * (1 shl reconnectAttempt.coerceAtMost(5))
        Log.d(TAG, "Reconnecting in ${delayMs}ms (attempt ${reconnectAttempt + 1})")
        scope.launch {
            delay(delayMs)
            if (destroyed) return@launch
            reconnectAttempt++
            try {
                connectGateway()
                reconnectAttempt = 0
                gaveUpReconnecting = false
            } catch (e: Exception) {
                Log.e(TAG, "Reconnect failed", e)
                scheduleReconnect()
            }
        }
    }

    private fun handleFrame(frame: JSONObject) {
        Log.d(TAG, "Received frame: $frame")

        when (frame.optString("type")) {
            "res" -> handleResponse(frame)
            "event" -> handleEvent(frame)
        }
    }

    private fun handleResponse(frame: JSONObject) {
        val ok = frame.optBoolean("ok", false)
        if (!ok) {
            Log.e(TAG, "Gateway response error: ${frame.optJSONObject("error")}")
            return
        }
        val payload = frame.optJSONObject("payload") ?: return

        // hello-ok from connect
        if (payload.has("protocol")) {
            connected = true
            reconnectAttempt = 0
            gaveUpReconnecting = false
            connectDeferred?.complete(true)
            Log.w(TAG, "Connected to OpenClaw gateway (protocol ${payload.optInt("protocol")})")
            subscribeToSession()
        }
    }

    private fun subscribeToSession() {
        send(JSONObject().apply {
            put("type", "req")
            put("id", nextId())
            put("method", "node.event")
            put("params", JSONObject().apply {
                put("event", "chat.subscribe")
                put("payloadJSON", JSONObject().apply {
                    put("sessionKey", SESSION_KEY)
                }.toString())
            })
        })
        Log.d(TAG, "Subscribed to session '$SESSION_KEY'")
    }

    private fun handleEvent(frame: JSONObject) {
        val event = frame.optString("event")
        val payload = frame.optJSONObject("payload")

        Log.d(TAG, "Received event: $event")

        when (event) {
            "connect.challenge" -> {
                // Ignored — connect request already sent (see TODO in connectGateway)
                Log.d(TAG, "Received connect.challenge (ignored, connect already sent)")
            }

            "chat" -> {
                if (payload != null) handleChatEvent(payload)
            }
        }
    }

    private fun handleChatEvent(payload: JSONObject) {
        val state = payload.optString("state")
        val runId = payload.optString("runId")

        // The gateway assigns its own runId which won't match our client-generated
        // key in pendingRuns, so fall back to activeRunId for callback lookup.
        val callback = pendingRuns[runId] ?: activeRunId?.let { pendingRuns[it] }

        val message = payload.optJSONObject("message")
        val content = message?.optJSONArray("content")
        val text = if (content != null && content.length() > 0) {
            content.getJSONObject(0).optString("text", "")
        } else ""

        when (state) {
            "delta" -> {
                // Ignored — delta events contain accumulated text, not individual tokens,
                // which breaks speakIncremental TTS. We only deliver the final response.
            }

            "final" -> {
                if (callback != null) {
                    try {
                        callback.onCompleteResponse(LlmResponse().apply {
                            this.text = text
                            this.toolCalls = emptyArray()
                        })
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to deliver complete response: ${e.message}")
                    }
                    activeRunId?.let { pendingRuns.remove(it) }
                    activeRunId = null
                }
            }

            "error", "aborted" -> {
                val errorMsg = payload.optString("errorMessage", "Agent run $state")
                callback?.onError(errorMsg)
                activeRunId?.let { pendingRuns.remove(it) }
                activeRunId = null
            }
        }
    }

    private val binder = object : ILlmService.Stub() {
        override fun setAvailableTools(tools: Array<ToolDefinition>) {
            // OpenClaw manages its own tools server-side; ignore.
        }

        override fun generateResponse(
            messages: Array<BinderConversationMessage>,
            tools: Array<ToolDefinition>,
            callback: ILlmCallback
        ) {
            scope.launch {
                if (!connected || ws == null) {
                    Log.d(TAG, "Not connected, attempting reconnect (5s deadline)")
                    reconnectAttempt = 0
                    gaveUpReconnecting = false

                    // If no reconnect is already in-flight, start one
                    if (connectDeferred == null || connectDeferred!!.isCompleted) {
                        try {
                            connectGateway()
                        } catch (_: Exception) {
                            connectDeferred?.complete(false)
                        }
                    }

                    val reconnected = withTimeoutOrNull(5000L) {
                        connectDeferred?.await()
                    } ?: false

                    if (!reconnected) {
                        reconnectAttempt = 0
                        callback.onError("Not connected to OpenClaw gateway")
                        return@launch
                    }
                }

                // Extract the last user message
                val lastUserMsg = messages.lastOrNull { it.type == "user" }
                val userMessage = lastUserMsg?.content
                if (userMessage.isNullOrBlank()) {
                    callback.onError("No user message found")
                    return@launch
                }

                // Encode image if present (vision mode: 2-finger hold)
                val imageBase64 = readImageBase64(lastUserMsg)

                val clientRunId = "pin-${UUID.randomUUID()}"
                pendingRuns[clientRunId] = callback
                activeRunId = clientRunId

                Log.d(TAG, "Sending to OpenClaw: \"$userMessage\"${if (imageBase64 != null) " [with image]" else ""}")

                send(JSONObject().apply {
                    put("type", "req")
                    put("id", nextId())
                    put("method", "node.event")
                    put("params", JSONObject().apply {
                        put("event", "agent.request")
                        put("payloadJSON", JSONObject().apply {
                            put("message", userMessage)
                            put("sessionKey", SESSION_KEY)
                            if (imageBase64 != null) {
                                put("images", org.json.JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("type", "image")
                                        put("data", imageBase64)
                                        put("mimeType", "image/jpeg")
                                    })
                                })
                            }
                        }.toString())
                    })
                })
            }
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun readImageBase64(message: BinderConversationMessage): String? {
        val pfd = message.imageFile ?: return null
        return try {
            val fd = pfd.fileDescriptor
            Os.lseek(fd, 0, OsConstants.SEEK_SET)
            val input = FileInputStream(fd)
            val buffer = ByteArray(4096)
            val output = ByteArrayOutputStream()
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
            }
            val encoded = Base64.Default.encode(output.toByteArray())
            Log.d(TAG, "Encoded image: ${output.size()} bytes -> ${encoded.length} chars base64")
            encoded
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read image", e)
            null
        }
    }

    private fun send(json: JSONObject) {
        ws?.send(WebSocketMessageType.TEXT, json.toString().toByteArray())
    }

    private var reqCounter = 0
    private fun nextId(): String = "pin-${++reqCounter}"

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        destroyed = true
        super.onDestroy()
        ws?.close()
        Log.d(TAG, "OpenClaw LLM service destroyed")
    }
}
