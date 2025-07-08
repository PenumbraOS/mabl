@file:OptIn(ExperimentalMaterial3Api::class)

package com.penumbraos.mabl

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.InputEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.penumbraos.mabl.discovery.PluginManager
import com.penumbraos.mabl.discovery.PluginService
import com.penumbraos.mabl.sdk.ILlmCallback
import com.penumbraos.mabl.sdk.ILlmService
import com.penumbraos.mabl.sdk.ISttCallback
import com.penumbraos.mabl.sdk.ISttService
import com.penumbraos.mabl.sdk.ITtsCallback
import com.penumbraos.mabl.sdk.ITtsService
import com.penumbraos.mabl.sdk.LlmResponse
import com.penumbraos.mabl.sdk.PluginConstants
import com.penumbraos.mabl.ui.AppDeviceDetector
import com.penumbraos.mabl.ui.SettingsScreen
import com.penumbraos.mabl.ui.UIComponents
import com.penumbraos.mabl.ui.UIFactory
import com.penumbraos.mabl.ui.android.AndroidConversationRenderer
import com.penumbraos.mabl.ui.android.AndroidInputHandler
import com.penumbraos.mabl.ui.android.AndroidNavigationController
import com.penumbraos.mabl.ui.android.AndroidScreen
import com.penumbraos.mabl.ui.theme.MABLTheme
import com.penumbraos.sdk.PenumbraClient
import com.penumbraos.sdk.api.types.TouchpadInputReceiver
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject


class MainActivity : ComponentActivity() {
    private var sttService: ISttService? = null
    private var ttsService: ITtsService? = null
    private var llmService: ILlmService? = null
    private lateinit var uiComponents: UIComponents
    private lateinit var deviceDetector: AppDeviceDetector
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

        // Initialize device detection and UI components
        deviceDetector = AppDeviceDetector()

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

            // Setup input handling based on device type
            setupInputHandling()
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
        val deviceType = deviceDetector.detect()
        val uiFactory = UIFactory(
            context = this,
            deviceType = deviceType,
            sttService = sttService,
            ttsService = ttsService
        )

        uiComponents = uiFactory.createUIComponents()
        Log.d("MainActivity", "UI components initialized for device type: $deviceType")
    }

    private fun setupInputHandling() {
        val deviceType = deviceDetector.detect()

        when (deviceType) {
            com.penumbraos.mabl.ui.DeviceType.AI_PIN -> {
                // Setup touchpad input for AI Pin
                try {
                    val client = PenumbraClient(applicationContext, true)
                    lifecycleScope.launch {
                        client.waitForBridge()
                        client.touchpad.register(object : TouchpadInputReceiver {
                            override fun onInputEvent(event: InputEvent) {
                                val motionEvent = event as MotionEvent
                                if (motionEvent.action == MotionEvent.ACTION_UP &&
                                    motionEvent.eventTime - motionEvent.downTime < 200
                                ) {
                                    Log.w("MainActivity", "Single touchpad tap detected")
                                    uiComponents.conversationRenderer.showListening(true)
                                    sttService?.startListening(sttCallback)
                                }
                            }
                        })
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to setup touchpad input", e)
                }
            }

            com.penumbraos.mabl.ui.DeviceType.PHONE -> {
                // Setup voice input callback for Android
                uiComponents.inputHandler.onVoiceInput { userInput ->
                    Log.d("MainActivity", "Voice input received: $userInput")
                    // This is handled by the STT callback
                }
            }
        }
    }
}

@Composable
fun PlatformUI(uiComponents: UIComponents) {
    val deviceType = AppDeviceDetector().detect()

    when (deviceType) {
        com.penumbraos.mabl.ui.DeviceType.AI_PIN -> {
            // AI Pin: Minimal UI, mostly handled by the renderer
            Text(
                text = "AI Pin Active - Use touchpad to interact",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        }

        com.penumbraos.mabl.ui.DeviceType.PHONE -> {
            // Android: Full UI with navigation
            AndroidUI(uiComponents)
        }
    }
}

@Composable
fun AndroidUI(uiComponents: UIComponents) {
    val navigationController = uiComponents.navigationController as AndroidNavigationController
    val conversationRenderer = uiComponents.conversationRenderer as AndroidConversationRenderer
    val inputHandler = uiComponents.inputHandler as AndroidInputHandler

    when (navigationController.currentScreen.value) {
        AndroidScreen.CONVERSATION -> {
            ConversationUI(
                conversation = conversationRenderer.conversationState.value,
                transcription = conversationRenderer.transcriptionState.value,
                isListening = conversationRenderer.listeningState.value,
                onStartListening = { inputHandler.startListening() },
                onStopListening = { inputHandler.stopListening() },
                onNavigateToPlugins = { navigationController.navigateToPluginDiscovery() }
            )
        }

        AndroidScreen.PLUGIN_DISCOVERY -> {
            PluginDiscoveryScreen(
                pluginManager = PluginManager(androidx.compose.ui.platform.LocalContext.current),
                onBack = { navigationController.goBack() }
            )
        }

        AndroidScreen.SETTINGS -> {
            SettingsScreen(onBack = { navigationController.goBack() })
        }
    }
}

@Composable
fun PluginDiscoveryScreen(pluginManager: PluginManager, onBack: () -> Unit = {}) {
    var plugins by remember { mutableStateOf<List<PluginService>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        plugins = pluginManager.discoverPlugins()
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MABL Plugin Discovery") },
                navigationIcon = {
                    if (onBack != {}) {
                        androidx.compose.material3.IconButton(onClick = onBack) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            plugins.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("No plugins found")
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                ) {
                    items(plugins) { plugin ->
                        PluginCard(plugin)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationUI(
    conversation: String,
    transcription: String,
    isListening: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onNavigateToPlugins: () -> Unit = {}
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Conversation:",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = conversation,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .padding(8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Current transcription: $transcription",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(16.dp))
        androidx.compose.foundation.layout.Row {
            Button(
                onClick = {
                    if (isListening) {
                        onStopListening()
                    } else {
                        onStartListening()
                    }
                }
            ) {
                Text(if (isListening) "Stop Listening" else "Start Listening")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onNavigateToPlugins
            ) {
                Text("Plugins")
            }
        }
    }
}

@Composable
fun PluginCard(service: PluginService) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = service.displayName ?: service.className,
                style = MaterialTheme.typography.titleMedium
            )
            service.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Text(
                text = "Type: ${service.type.name}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Package: ${service.packageName}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            service.tools?.let { tools ->
                if (tools.isNotEmpty()) {
                    Text(
                        text = "Tools: ${tools.joinToString()}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
