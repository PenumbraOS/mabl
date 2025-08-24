package com.penumbraos.mabl.aipincore.view.nav

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.open.pin.ui.components.button.PinCircularButton
import com.open.pin.ui.components.views.RadialView
import com.open.pin.ui.components.views.RadialViewParams
import com.open.pin.ui.debug.AiPinPreview
import com.penumbraos.mabl.R
import com.penumbraos.mabl.aipincore.view.model.ConversationsNav
import com.penumbraos.mabl.aipincore.view.model.HomeNav
import com.penumbraos.mabl.aipincore.view.model.PlatformViewModel

data class MenuItem(val icon: ImageVector, val view: Any)

@Composable
fun Menu(animatedRadius: Dp) {
    val viewModel = viewModel<PlatformViewModel>()

    val menuItems = listOf(
        MenuItem(Icons.Default.Home, HomeNav),
        MenuItem(ImageVector.vectorResource(R.drawable.outline_voice_chat_24), ConversationsNav),
        MenuItem(Icons.Default.Call, HomeNav),
        MenuItem(Icons.Default.Notifications, HomeNav),
        MenuItem(Icons.Default.Settings, HomeNav)
    )

    RadialView(
        Modifier
            .fillMaxSize()
            .background(color = Color(0f, 0f, 0f, 0.9f)),
        RadialViewParams(radius = animatedRadius),
        menuItems
    ) { item ->
        PinCircularButton({
            Log.d("Menu", "Navigating to ${item.view}")
            if (item.view == HomeNav) {
                viewModel.navViewModel.jumpHome()
            } else {
                viewModel.navViewModel.replaceLastView(item.view)
            }
        }, icon = item.icon)
    }
}

@AiPinPreview
@Composable
fun MenuPreview() {
    Menu(animatedRadius = 150.dp)
}