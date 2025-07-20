package com.penumbraos.mabl.aipincore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.open.pin.ui.PinTheme
import com.open.pin.ui.components.views.ListView
import com.open.pin.ui.theme.PinColors
import com.open.pin.ui.theme.PinTypography
import com.open.pin.ui.utils.modifiers.ProvideSnapCoordinator
import com.open.pin.ui.utils.modifiers.SnapCoordinator
import com.penumbraos.mabl.aipincore.view.TouchInterceptor
import com.penumbraos.mabl.data.AppDatabase
import com.penumbraos.mabl.data.Message
import com.penumbraos.mabl.data.MessageRepository
import com.penumbraos.mabl.ui.UIComponents

@Composable
fun PlatformUI(uiComponents: UIComponents) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember { MessageRepository(database.messageDao()) }
    val messages = repository.getAllMessages().collectAsState(initial = emptyList())
    val snapCoordinator = remember { mutableStateOf(SnapCoordinator()) }

    PinTheme {
        ProvideSnapCoordinator(coordinator = snapCoordinator.value) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    TouchInterceptor(snapCoordinator.value, context)
                }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = PinTheme.colors.background)
            ) {
                ConversationDisplay(
                    messages = messages.value,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun ConversationDisplay(
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MessageItem(
    message: Message,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (message.isUser) {
                PinColors.primary.copy(alpha = 0.1f)
            } else {
                PinColors.secondary.copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            PinText(
                text = if (message.isUser) "You" else "Assistant",
                style = PinTypography.labelSmall,
                color = if (message.isUser) PinColors.primary else PinColors.secondary
            )
            PinText(
                text = message.content,
                style = PinTypography.displaySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
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