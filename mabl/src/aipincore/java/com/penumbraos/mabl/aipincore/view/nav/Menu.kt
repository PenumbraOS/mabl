package com.penumbraos.mabl.aipincore.view.nav

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.open.pin.ui.components.button.PinCircularButton
import com.open.pin.ui.components.views.RadialView
import com.open.pin.ui.components.views.RadialViewParams

@Composable
fun Menu(animatedRadius: Dp) {
    RadialView(
        Modifier
            .fillMaxSize()
            .background(color = Color(0f, 0f, 0f, 0.9f)),
        RadialViewParams(radius = animatedRadius),
        listOf(
            Icons.Default.Home,
            Icons.Default.Email,
            Icons.Default.Call,
            Icons.Default.Notifications,
            Icons.Default.Settings
        )
    ) { icon ->
        PinCircularButton({
            Log.d("PinMainView", "Button clicked")
        }, icon = icon)
    }
}
