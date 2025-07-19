package com.penumbraos.mabl.simulation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.penumbraos.mabl.ui.PlatformUI
import com.penumbraos.mabl.ui.UIComponents

@Composable
fun SimulatedPinDisplay(
    modifier: Modifier = Modifier,
    uiComponents: UIComponents
) {
    val density = LocalDensity.current

    // Calculate 800x720 aspect ratio while fitting within available space
    val targetAspectRatio = 800f / 720f

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val availableWidth = maxWidth
        val availableHeight = maxHeight

        // Calculate display size maintaining 800x720 aspect ratio
        val (displayWidth, displayHeight) = with(density) {
            val widthPx = (availableWidth.toPx() * 0.9f).toInt()
            val heightPx = (availableHeight.toPx() * 0.9f).toInt()

            val scaledHeight = (widthPx / targetAspectRatio).toInt()
            val scaledWidth = (heightPx * targetAspectRatio).toInt()

            if (scaledHeight <= heightPx) {
                Pair(widthPx.toDp(), scaledHeight.toDp())
            } else {
                Pair(scaledWidth.toDp(), heightPx.toDp())
            }
        }

        // Simulated Pin Display Container
        Box(
            modifier = Modifier
                .size(displayWidth, displayHeight)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, Color.Gray, RoundedCornerShape(12.dp))
                .background(Color.Black)
        ) {
            // Render the actual AI Pin UI content
            PlatformUI(uiComponents)
        }

        // Display dimensions indicator
        Text(
            text = "${displayWidth.value.toInt()}x${displayHeight.value.toInt()} (scaled from 800x720)",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(top = 8.dp)
        )
    }
}