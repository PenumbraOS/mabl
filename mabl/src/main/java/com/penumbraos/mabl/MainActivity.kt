package com.penumbraos.mabl

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.penumbraos.mabl.sdk.ILlmCallback
import com.penumbraos.mabl.sdk.ISttCallback
import com.penumbraos.mabl.sdk.LlmResponse
import com.penumbraos.mabl.services.AllControllers
import com.penumbraos.mabl.types.Error
import com.penumbraos.mabl.ui.PlatformUI
import com.penumbraos.mabl.ui.UIComponents
import com.penumbraos.mabl.ui.UIFactory
import com.penumbraos.mabl.ui.theme.MABLTheme
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject


class MainActivity : ComponentActivity() {
    private val controllers = AllControllers()
    private lateinit var uiComponents: UIComponents

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
                controllers.tts.service?.speakIncremental(finalText)
                uiComponents.conversationRenderer.showMessage(finalText, isUser = true)
                uiComponents.conversationRenderer.showListening(false)

                controllers.llm.service?.generateResponse(
                    "$finalText /no_think",
                    object : ILlmCallback.Stub() {
                        override fun onPartialResponse(newToken: String) {
                            Log.i("MainActivity", "LLM partial response: $newToken")
                            controllers.tts.service?.speakIncremental(newToken)
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
                                                controllers.tts.service?.speakIncremental("Timer set for $duration")
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
                            uiComponents.conversationRenderer.showError(Error.LlmError(error))
                        }
                    })
            }
        }

        override fun onError(errorMessage: String) {
            runOnUiThread {
                uiComponents.conversationRenderer.showError(Error.SttError(errorMessage))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        controllers.initialize()

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

        initializeUIComponents()

        lifecycleScope.launch {
            controllers.connectAll(this@MainActivity)
            controllers.stt.delegate = sttCallback

            // Setup input handling
            uiComponents.inputHandler.setup(
                context = this@MainActivity,
                lifecycleScope = lifecycleScope,
                sttService = controllers.stt.service,
                sttCallback = sttCallback,
                conversationRenderer = uiComponents.conversationRenderer
            )
        }
    }

    override fun onStop() {
        super.onStop()
        controllers.shutdown(this)
    }

    private fun initializeUIComponents() {
        val uiFactory = UIFactory(
            context = this,
            controllers
        )

        uiComponents = uiFactory.createUIComponents()
        Log.d("MainActivity", "UI components initialized")
    }

}

