package com.penumbraos.mabl.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.penumbraos.mabl.discovery.PluginManager
import com.penumbraos.mabl.discovery.PluginService

@Composable
fun PlatformUI(uiComponents: UIComponents) {
    val conversationRenderer = uiComponents.conversationRenderer as ConversationRenderer
    val inputHandler = uiComponents.inputHandler

    // when (navigationController.currentScreen.value) {
    //     AndroidScreen.CONVERSATION -> {
    //         ConversationUI(
    //             conversation = conversationRenderer.conversationState.value,
    //             transcription = conversationRenderer.transcriptionState.value,
    //             isListening = conversationRenderer.listeningState.value,
    //             onStartListening = { inputHandler.startListening() },
    //             onStopListening = { inputHandler.stopListening() },
    //             onNavigateToPlugins = {  }
    //         )
    //     }

    //     AndroidScreen.PLUGIN_DISCOVERY -> {
    //         PluginDiscoveryScreen(
    //             pluginManager = PluginManager(LocalContext.current),
    //             onBack = {  }
    //         )
    //     }

    //     AndroidScreen.SETTINGS -> {
    //         SettingsScreen(onBack = {  })
    //     }
    // }
}

@Composable
private fun ConversationUI(
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
        Row {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PluginDiscoveryScreen(pluginManager: PluginManager, onBack: () -> Unit = {}) {
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
                        IconButton(onClick = onBack) {
                            Icon(
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
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            plugins.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
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
private fun PluginCard(service: PluginService) {
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
        }
    }
}