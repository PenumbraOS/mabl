package com.penumbraos.mabl.simulation

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.penumbraos.mabl.ui.UIComponents

@SuppressLint("UnusedBoxWithConstraintsScope")
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
        val (displayWidth, displayHeight, scaleFactor) = with(density) {
            val availableWidthPx = availableWidth.toPx() * 0.9f
            val availableHeightPx = availableHeight.toPx() * 0.9f

            val scaledHeight = availableWidthPx / targetAspectRatio
            val scaledWidth = availableHeightPx * targetAspectRatio

            if (scaledHeight <= availableHeightPx) {
                val scale = availableWidthPx / (800f * density.density)
                Triple(availableWidthPx.toDp(), scaledHeight.toDp(), scale)
            } else {
                val scale = availableHeightPx / (720f * density.density)
                Triple(scaledWidth.toDp(), availableHeightPx.toDp(), scale)
            }
        }

        // Simulated Pin Display Container
        Box(
            modifier = Modifier
                .requiredSize(displayWidth, displayHeight)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, Color.Gray, RoundedCornerShape(12.dp))
                .background(Color.Black)
        ) {
            Box(
                modifier = Modifier
                    .requiredSize(800.dp, 720.dp)
                    .graphicsLayer(
                        scaleX = scaleFactor,
                        scaleY = scaleFactor,
                        transformOrigin = TransformOrigin.Center
                    )
            ) {
                com.penumbraos.mabl.aipincore.PlatformUI(uiComponents)
            }
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