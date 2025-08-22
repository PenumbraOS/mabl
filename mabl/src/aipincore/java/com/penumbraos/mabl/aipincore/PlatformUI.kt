package com.penumbraos.mabl.aipincore

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.open.pin.ui.PinTheme
import com.open.pin.ui.components.button.PinCircularButton
import com.open.pin.ui.components.views.ListView
import com.open.pin.ui.components.views.RadialView
import com.open.pin.ui.components.views.RadialViewParams
import com.open.pin.ui.theme.PinColors
import com.open.pin.ui.theme.PinFonts
import com.open.pin.ui.theme.PinTypography
import com.open.pin.ui.utils.PinDimensions
import com.open.pin.ui.utils.modifiers.ProvideSnapCoordinator
import com.open.pin.ui.utils.modifiers.SnapCoordinator
import com.penumbraos.mabl.aipincore.view.PlatformViewModel
import com.penumbraos.mabl.aipincore.view.TouchInterceptor
import com.penumbraos.mabl.data.AppDatabase
import com.penumbraos.mabl.data.Message
import com.penumbraos.mabl.data.MessageRepository
import com.penumbraos.mabl.ui.UIComponents

@Composable
fun PlatformUI(uiComponents: UIComponents) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val snapCoordinator = remember { mutableStateOf(SnapCoordinator()) }
    val viewModel = uiComponents.platformCapabilities.getViewModel() as? PlatformViewModel
        ?: remember { PlatformViewModel() }

    PinTheme {
        ProvideSnapCoordinator(coordinator = snapCoordinator.value) {
            Box(modifier = Modifier.fillMaxSize()) {
                // For some very strange reason things on the bottom are higher z-index
                PinMainView(uiComponents, database, viewModel)
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context -> TouchInterceptor(snapCoordinator.value, context) }
                )
            }
        }
    }
}

@Composable
fun PinMainView(
    uiComponents: UIComponents,
    database: AppDatabase,
    viewModel: PlatformViewModel
) {
    val menuVisible by viewModel.menuVisibleState.collectAsState()

    val animatedRadius by animateDpAsState(
        targetValue = if (menuVisible) 150.dp else 300.dp,
        label = "animatedRadius"
    )

    val repository = remember { MessageRepository(database.messageDao()) }
    val messages = repository.getAllMessages().collectAsState(initial = emptyList())

    ConversationDisplay(messages = messages.value)
    AnimatedVisibility(
        visible = menuVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
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
}

@Composable
fun ConversationDisplay(
    modifier: Modifier = Modifier,
    messages: List<Message>,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = PinTheme.colors.background)
    ) {
        ConversationList(
            messages = messages,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun ConversationList(
    messages: List<Message>,
    modifier: Modifier = Modifier
) {
    if (messages.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            PinText(
                text = "No Conversation",
                style = TextStyle(fontSize = 24.sp),
                textAlign = TextAlign.Center
            )
        }
    } else {
        ListView(
            showScrollButtons = true,
            autoHideButtons = true
        ) {
            Column {
                for (message in messages) {
                    MessageItem(
                        message = message,
                    )
                }
            }
        }
    }
}

@Composable
fun MessageItem(
    message: Message,
) {
    Column(
        modifier = Modifier.padding(12.dp)
    ) {
        PinText(
            text = if (message.isUser) "You" else "MABL",
            style = PinTypography.bodyLarge,
            color = PinColors.secondary
        )
        PinText(
            text = message.content,
            style = TextStyle(
                fontFamily = PinFonts.Poppins,
                fontWeight = PinDimensions.fontWeightBold,
                fontSize = PinDimensions.fontSizeExtraLarge,
                lineHeight = 84.sp,
                letterSpacing = 0.5.sp
            ),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun PinText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = PinColors.primary,
    style: TextStyle = PinTypography.bodyLarge,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = style,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines
    )
}

@Preview(widthDp = 800, heightDp = 720)
@Composable
fun ConversationDisplayPreview() {
    val messages = listOf(
        Message(1, "Hello!", true),
        Message(
            2,
            "Next week in Seoul, expect showers on Thursday morning with temperatures ranging from 25 to 30 degrees. It will be sunny the rest of the week.",
            false
        )
    )

    val snapCoordinator = remember { mutableStateOf(SnapCoordinator()) }

    PinTheme {
        ProvideSnapCoordinator(coordinator = snapCoordinator.value) {
            ConversationDisplay(messages = messages)
        }
    }
}