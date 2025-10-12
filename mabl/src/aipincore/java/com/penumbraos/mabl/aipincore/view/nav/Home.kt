package com.penumbraos.mabl.aipincore.view.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.open.pin.ui.components.text.PinText
import com.open.pin.ui.debug.AiPinPreview
import com.open.pin.ui.theme.PinTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun Home() {
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val zone = ZoneId.systemDefault()
            while (isActive) {
                val currentTimeMillis = System.currentTimeMillis()

                currentTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(currentTimeMillis), zone)
                // Tick every ~100ms, normalized based on current time
                val nextWall = ((currentTimeMillis / 100) + 1) * 100
                val delayMs = (nextWall - currentTimeMillis).coerceIn(0, 100)
                delay(delayMs)
            }
        }
    }

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        PinText(
            text = currentTime.format(timeFormatter),
            style = TextStyle(fontSize = 160.sp),
            textAlign = TextAlign.Center
        )

        PinText(
            text = currentTime.format(dateFormatter),
            style = PinTypography.displayMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(y = (-120).dp)
        )
    }
}

@AiPinPreview
@Composable
fun HomePreview() {
    Home()
}
