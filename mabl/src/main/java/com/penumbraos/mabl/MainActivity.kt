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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.penumbraos.mabl.interaction.InteractionContentCallback
import com.penumbraos.mabl.interaction.InteractionStateCallback
import com.penumbraos.mabl.services.AllControllers
import com.penumbraos.mabl.types.Error
import com.penumbraos.mabl.ui.PlatformUI
import com.penumbraos.mabl.ui.UIComponents
import com.penumbraos.mabl.ui.UIFactory
import com.penumbraos.mabl.ui.theme.MABLTheme
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    private val controllers = AllControllers()
    private lateinit var uiComponents: UIComponents

    private val interactionStateCallback = object : InteractionStateCallback {
        override fun onListeningStarted() {
            runOnUiThread {
                Log.i("MainActivity", "Listening started")
                uiComponents.conversationRenderer.showListening(true)
            }
        }

        override fun onListeningStopped() {
            runOnUiThread {
                Log.i("MainActivity", "Listening stopped")
                uiComponents.conversationRenderer.showListening(false)
            }
        }

        override fun onProcessingStarted() {
            runOnUiThread {
                Log.i("MainActivity", "Processing started")
                // Could show processing indicator here
            }
        }

        override fun onProcessingStopped() {
            runOnUiThread {
                Log.i("MainActivity", "Processing stopped")
            }
        }

        override fun onSpeakingStarted() {
            runOnUiThread {
                Log.i("MainActivity", "Speaking started")
                // Could show speaking indicator here
            }
        }

        override fun onSpeakingStopped() {
            runOnUiThread {
                Log.i("MainActivity", "Speaking stopped")
            }
        }

        override fun onFlowCancelled() {
            runOnUiThread {
                Log.i("MainActivity", "Flow cancelled")
                uiComponents.conversationRenderer.showListening(false)
            }
        }

        override fun onError(error: Error) {
            runOnUiThread {
                uiComponents.conversationRenderer.showError(error)
            }
        }
    }

    private val interactionContentCallback = object : InteractionContentCallback {
        override fun onPartialTranscription(text: String) {
            runOnUiThread {
                Log.i("MainActivity", "Partial transcription: $text")
                uiComponents.conversationRenderer.showTranscription(text)
            }
        }

        override fun onFinalTranscription(text: String) {
            runOnUiThread {
                Log.i("MainActivity", "Final transcription: $text")
                uiComponents.conversationRenderer.showMessage(text, isUser = true)
            }
        }

        override fun onPartialResponse(token: String) {
            runOnUiThread {
                Log.i("MainActivity", "Partial response: $token")
                // Could show partial response in UI
            }
        }

        override fun onFinalResponse(response: String) {
            runOnUiThread {
                Log.i("MainActivity", "Final response: $response")
                uiComponents.conversationRenderer.showMessage(response, isUser = false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        controllers.initialize(this)

        initializeUIComponents()

        lifecycleScope.launch {
            controllers.connectAll(this@MainActivity)

            controllers.interactionFlowManager.setStateCallback(interactionStateCallback)
            controllers.interactionFlowManager.setContentCallback(interactionContentCallback)

            // TODO: Allow renewing conversation
            val conversationManager = controllers.conversationSessionManager.startNewConversation()
            controllers.interactionFlowManager.setConversationManager(conversationManager)

            uiComponents.platformInputHandler.setup(
                context = this@MainActivity,
                lifecycleScope = lifecycleScope,
                interactionFlowManager = controllers.interactionFlowManager
            )

            Log.d("MainActivity", "Centralized interaction flow initialized")
        }

        enableEdgeToEdge()

        setContent {
            MABLTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Content()
                }
            }
        }
    }

    @Composable
    fun Content() {
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

    override fun onDestroy() {
        super.onDestroy()
        controllers.shutdown(this)
    }

    private fun initializeUIComponents() {
        val uiFactory = UIFactory(
            lifecycleScope,
            context = this,
            controllers,
        )

        uiComponents = uiFactory.createUIComponents()
        Log.d("MainActivity", "UI components initialized")
    }

}

