package com.penumbraos.mabl

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.penumbraos.mabl.sdk.ILlmCallback
import com.penumbraos.mabl.sdk.ILlmService
import com.penumbraos.mabl.sdk.ISttCallback
import com.penumbraos.mabl.sdk.ISttService
import com.penumbraos.mabl.sdk.ITtsCallback
import com.penumbraos.mabl.sdk.ITtsService
import com.penumbraos.mabl.sdk.LlmResponse
import com.penumbraos.mabl.sdk.PluginConstants
import com.penumbraos.mabl.ui.UIComponents
import com.penumbraos.mabl.ui.UIFactory
import com.penumbraos.mabl.ui.theme.MABLTheme
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject


class MainActivity : ComponentActivity() {
    private var sttService: ISttService? = null
    private var ttsService: ITtsService? = null
    private var llmService: ILlmService? = null
    private lateinit var uiComponents: UIComponents
    private val sttConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d("MainActivity", "onServiceConnected: stt")
            sttService = ISttService.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            sttService = null
        }
    }
    private val ttsConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d("MainActivity", "onServiceConnected: tts")
            ttsService = ITtsService.Stub.asInterface(service)
            ttsService?.registerCallback(object : ITtsCallback.Stub() {
                override fun onSpeechStarted() {}
                override fun onSpeechFinished() {}
                override fun onError(errorMessage: String) {
                    runOnUiThread {
                        if (::uiComponents.isInitialized) {
                            uiComponents.conversationRenderer.showError("TTS Error: $errorMessage")
                        }
                    }
                }
            })
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            ttsService = null
        }
    }
    private val llmConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d("MainActivity", "onServiceConnected: llm")
            llmService = ILlmService.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            llmService = null
        }
    }

    private val sttCallback = object : ISttCallback.Stub() {
        override fun onPartialTranscription(partialText: String) {
            runOnUiThread {
                Log.i("MainActivity", "STT partial transcription: $partialText")
                uiComponents.conversationRenderer.showTranscription(partialText)
            }
        }

        override fun onFinalTranscription(finalText: String) {
            runOnUiThread {
                Log.w("MainActivity", "LLM request: $finalText")
                ttsService?.speakIncremental(finalText)
                uiComponents.conversationRenderer.showMessage(finalText, isUser = true)
                uiComponents.conversationRenderer.showListening(false)

                llmService?.generateResponse("$finalText /no_think", object : ILlmCallback.Stub() {
                    override fun onPartialResponse(newToken: String) {
                        Log.i("MainActivity", "LLM partial response: $newToken")
                        ttsService?.speakIncremental(newToken)
                    }

                    override fun onCompleteResponse(response: LlmResponse) {
                        runOnUiThread {
                            val responseText = response.text ?: "No response text"
                            uiComponents.conversationRenderer.showMessage(
                                responseText,
                                isUser = false
                            )

                            if (response.toolCalls.isNotEmpty()) {
                                response.toolCalls.forEach { toolCall ->
                                    if (toolCall.name == "create_timer") {
                                        try {
                                            val parameters =
                                                Json.decodeFromString<JsonObject>(toolCall.parameters)
                                            val duration = parameters["duration"]
                                            ttsService?.speakIncremental("Timer set for $duration")
                                        } catch (e: Exception) {
                                            Log.e(
                                                "MainActivity",
                                                "Error parsing tool call parameters",
                                                e
                                            )
                                        }
                                    }
                                    uiComponents.conversationRenderer.showMessage(
                                        "TOOL_CALL: ${toolCall.name}, parameters: ${toolCall.parameters}",
                                        isUser = false
                                    )
                                }
                            }
                        }
                    }

                    override fun onError(error: String) {
                        Log.w("MainActivity", "LLM error: $error")
                        uiComponents.conversationRenderer.showError(error)
                    }
                })
            }
        }

        override fun onError(errorMessage: String) {
            runOnUiThread {
                uiComponents.conversationRenderer.showError(errorMessage)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MABLTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (::uiComponents.isInitialized) {
                        PlatformUI(uiComponents)
                    } else {
                        // Show loading state while services are connecting
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            val sttIntent = Intent(PluginConstants.ACTION_STT_SERVICE).apply {
                setPackage("com.penumbraos.plugins.demo")
            }

            val ttsIntent = Intent(PluginConstants.ACTION_TTS_SERVICE).apply {
                setPackage("com.penumbraos.plugins.demo")
            }

            val llmIntent = Intent(PluginConstants.ACTION_LLM_SERVICE).apply {
                setPackage("com.penumbraos.plugins.openai")
            }

            // Force services to be active using foreground service
            startForegroundService(sttIntent)
            startForegroundService(ttsIntent)
            startForegroundService(llmIntent)

            if (!bindService(
                    sttIntent,
                    sttConnection,
                    BIND_AUTO_CREATE
                )
            ) {
                Log.e("MainActivity", "Could not set up binding for STT service")
            }

            if (!bindService(
                    ttsIntent,
                    ttsConnection,
                    BIND_AUTO_CREATE
                )
            ) {
                Log.e("MainActivity", "Could not set up binding for TTS service")
            }

            if (!bindService(
                    llmIntent,
                    llmConnection,
                    BIND_AUTO_CREATE
                )
            ) {
                Log.e("MainActivity", "Could not set up binding for LLM service")
            }

            // Initialize UI components after services are bound
            initializeUIComponents()

            // Setup input handling
            uiComponents.inputHandler.setup(
                context = this@MainActivity,
                lifecycleScope = lifecycleScope,
                sttService = sttService,
                sttCallback = sttCallback,
                conversationRenderer = uiComponents.conversationRenderer
            )
        }
    }

    override fun onStop() {
        super.onStop()
        if (sttService != null) {
            unbindService(sttConnection)
        }
        if (ttsService != null) {
            unbindService(ttsConnection)
        }
        if (llmService != null) {
            unbindService(llmConnection)
        }
    }

    private fun initializeUIComponents() {
        val uiFactory = UIFactory(
            context = this,
        )

        uiComponents = uiFactory.createUIComponents()
        Log.d("MainActivity", "UI components initialized")
    }

}

@Composable
fun PlatformUI(uiComponents: UIComponents) {
    com.penumbraos.mabl.ui.PlatformUI(uiComponents)
}

