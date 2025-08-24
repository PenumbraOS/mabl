package com.penumbraos.mabl.aipincore.view.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.open.pin.ui.components.text.PinText
import com.open.pin.ui.debug.AiPinPreview
import com.open.pin.ui.theme.PinTypography
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Timer
import java.util.TimerTask

@Composable
fun Home() {
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }

    DisposableEffect(Unit) {
        val timer = Timer()
        val task = object : TimerTask() {
            override fun run() {
                currentTime = LocalDateTime.now()
            }
        }
        timer.scheduleAtFixedRate(task, 0, 1000)

        onDispose {
            timer.cancel()
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
