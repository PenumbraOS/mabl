package com.penumbraos.mabl.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.penumbraos.mabl.simulation.ControlPanel
import com.penumbraos.mabl.simulation.SimulatedPinDisplay
import com.penumbraos.mabl.simulation.SimulatedTouchpad

@Composable
fun PlatformUI(uiComponents: UIComponents) {
    // AI Pin Simulator: Three-panel layout for development and testing
    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        // Left Panel: Simulated 800x720 Pin Display
        Card(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = "AI Pin Display (800x720)",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SimulatedPinDisplay(
                    modifier = Modifier.fillMaxSize(),
                    uiComponents = uiComponents
                )
            }
        }

        // Center Panel: Touchpad Simulation
        Card(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = "Touchpad",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SimulatedTouchpad(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Right Panel: Simulator Controls
        Card(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Simulator Controls",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ControlPanel(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}