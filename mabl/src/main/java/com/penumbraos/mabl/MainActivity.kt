@file:OptIn(ExperimentalMaterial3Api::class)

package com.penumbraos.mabl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.penumbraos.mabl.discovery.PluginManager
import com.penumbraos.mabl.discovery.PluginService
import com.penumbraos.mabl.ui.theme.MABLTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MABLTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PluginDiscoveryScreen(PluginManager(this))
                }
            }
        }
    }
}

@Composable
fun PluginDiscoveryScreen(pluginManager: PluginManager) {
    var plugins by remember { mutableStateOf<List<PluginService>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        plugins = pluginManager.discoverPlugins()
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("MABL Plugin Discovery") })
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
