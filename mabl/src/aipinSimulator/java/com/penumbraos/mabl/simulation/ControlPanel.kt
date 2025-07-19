package com.penumbraos.mabl.simulation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.penumbraos.mabl.ui.SimulatorEventRouter
import com.penumbraos.mabl.ui.SimulatorSttRouter

enum class SttMode {
    MANUAL,      // Manual text input
    LIVE_STT     // Use Android's built-in STT
}

@Composable
fun ControlPanel(
    modifier: Modifier = Modifier
) {
    var sttMode by remember { mutableStateOf(SttMode.MANUAL) }
    var manualText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf("Connected") }
    var lastEvent by remember { mutableStateOf("Ready") }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Connection Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (connectionStatus) {
                    "Connected" -> MaterialTheme.colorScheme.primaryContainer
                    "Disconnected" -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (connectionStatus == "Connected") Icons.Default.Check else Icons.Default.Close,
                    contentDescription = connectionStatus,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = connectionStatus,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        // STT Mode Selection
        Text(
            text = "STT Input Mode",
            style = MaterialTheme.typography.titleSmall
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = sttMode == SttMode.MANUAL,
                onClick = { sttMode = SttMode.MANUAL },
                label = { Text("Manual") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Manual input",
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.weight(1f)
            )

            FilterChip(
                selected = sttMode == SttMode.LIVE_STT,
                onClick = { sttMode = SttMode.LIVE_STT },
                label = { Text("Live STT") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Live STT",
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }

        // Input Controls based on mode
        when (sttMode) {
            SttMode.MANUAL -> {
                OutlinedTextField(
                    value = manualText,
                    onValueChange = { manualText = it },
                    label = { Text("Enter query") },
                    placeholder = { Text("What time is it?") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (manualText.isNotBlank()) {
                                SimulatorSttRouter.instance?.onSimulatorManualInput(manualText)
                                lastEvent = "Manual: \"$manualText\""
                                manualText = ""
                            }
                        }
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (manualText.isNotBlank()) {
                                    SimulatorSttRouter.instance?.onSimulatorManualInput(manualText)
                                    lastEvent = "Manual: \"$manualText\""
                                    manualText = ""
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                )
            }

            SttMode.LIVE_STT -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (!isListening) {
                                isListening = true
                                lastEvent = "Starting live STT..."
                                SimulatorSttRouter.instance?.onSimulatorStartListening()
                            }
                        },
                        enabled = !isListening,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start listening")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start")
                    }

                    Button(
                        onClick = {
                            if (isListening) {
                                isListening = false
                                lastEvent = "Stopping live STT..."
                                SimulatorSttRouter.instance?.onSimulatorStopListening()
                            }
                        },
                        enabled = isListening,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Stop listening")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop")
                    }
                }

                if (isListening) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Listening",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Listening...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }

        // Quick Actions
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleSmall
        )

        val quickPhrases = listOf(
            "What time is it?",
            "Set a timer for 5 minutes",
            "Tell me a joke",
            "What's the weather?"
        )

        quickPhrases.forEach { phrase ->
            OutlinedButton(
                onClick = {
                    SimulatorSttRouter.instance?.onSimulatorManualInput(phrase)
                    lastEvent = "Quick: \"$phrase\""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = phrase,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Event Log
        Text(
            text = "Last Event",
            style = MaterialTheme.typography.titleSmall
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = lastEvent,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp)
            )
        }

        // Gesture Shortcuts
        Text(
            text = "Gesture Shortcuts",
            style = MaterialTheme.typography.titleSmall
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    SimulatorEventRouter.instance?.let { router ->
                        // Simulate single tap
                        val currentTime = System.currentTimeMillis()
                        val downEvent = android.view.MotionEvent.obtain(
                            currentTime,
                            currentTime,
                            android.view.MotionEvent.ACTION_DOWN,
                            0f,
                            0f,
                            0
                        )
                        val upEvent = android.view.MotionEvent.obtain(
                            currentTime,
                            currentTime + 100,
                            android.view.MotionEvent.ACTION_UP,
                            0f,
                            0f,
                            0
                        )
                        router.onSimulatorTouchpadEvent(downEvent)
                        router.onSimulatorTouchpadEvent(upEvent)
                        downEvent.recycle()
                        upEvent.recycle()
                        lastEvent = "Simulated single tap"
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Tap", style = MaterialTheme.typography.labelSmall)
            }

            OutlinedButton(
                onClick = {
                    SimulatorEventRouter.instance?.let { router ->
                        // Simulate long press
                        val currentTime = System.currentTimeMillis()
                        val downEvent = android.view.MotionEvent.obtain(
                            currentTime,
                            currentTime,
                            android.view.MotionEvent.ACTION_DOWN,
                            0f,
                            0f,
                            0
                        )
                        val upEvent = android.view.MotionEvent.obtain(
                            currentTime,
                            currentTime + 1500,
                            android.view.MotionEvent.ACTION_UP,
                            0f,
                            0f,
                            0
                        )
                        router.onSimulatorTouchpadEvent(downEvent)
                        router.onSimulatorTouchpadEvent(upEvent)
                        downEvent.recycle()
                        upEvent.recycle()
                        lastEvent = "Simulated long press"
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Hold", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

