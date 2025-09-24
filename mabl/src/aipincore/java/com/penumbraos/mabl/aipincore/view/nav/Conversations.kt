package com.penumbraos.mabl.aipincore.view.nav

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.open.pin.ui.PinTheme
import com.open.pin.ui.components.button.PinButtonBase
import com.open.pin.ui.components.button.PinButtonShapeConfig
import com.open.pin.ui.components.text.PinText
import com.open.pin.ui.components.views.ListView
import com.open.pin.ui.theme.PinColors
import com.open.pin.ui.theme.PinFonts
import com.open.pin.ui.theme.PinTypography
import com.open.pin.ui.utils.PinDimensions
import com.penumbraos.mabl.aipincore.view.model.ConversationsViewModel
import com.penumbraos.mabl.aipincore.view.model.NavViewModel
import com.penumbraos.mabl.aipincore.view.model.PlatformViewModel
import com.penumbraos.mabl.aipincore.view.util.formatRelativeTimestamp
import com.penumbraos.mabl.data.types.Conversation

@Composable
fun Conversations() {
    val viewModel = viewModel<PlatformViewModel>()

    val conversationsViewModel = remember { ConversationsViewModel(viewModel) }

    viewModel<ConversationsViewModel>(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return conversationsViewModel as T
        }
    })

    val conversations = conversationsViewModel.conversationsWithInjectedTitle.collectAsState(
        emptyList()
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = PinTheme.colors.background)
    ) {
        AllConversationsList(
            conversations = conversations.value
        )
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun AllConversationsList(
    modifier: Modifier = Modifier,
    conversations: List<Conversation>,
) {
    val navViewModel = viewModel<NavViewModel>()

    val menuOpen by navViewModel.isMenuOpen

    if (conversations.isEmpty()) {
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
            showScrollButtons = !menuOpen,
            autoHideButtons = true
        ) {
            for (conversation in conversations) {
                ConversationTitleCard(
                    conversation = conversation,
                )
            }
        }
    }
}

@Composable
fun ConversationTitleCard(
    conversation: Conversation,
) {
    val viewModel = viewModel<ConversationsViewModel>()

    PinButtonBase(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        shapeConfig = PinButtonShapeConfig(
            shape = RoundedCornerShape(PinDimensions.buttonCornerRadius),
            minSize = DpSize(0.dp, PinDimensions.buttonHeightPrimary)
        ),
        onClick = {
            viewModel.openConversation(conversation.id)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Using PinText breaks the colors
            Text(
                // TODO: Rerender every minute to keep this up to date
                text = formatRelativeTimestamp(conversation.createdAt),
                style = PinTypography.bodyLarge,
                color = PinColors.secondary
            )
            Text(
                text = conversation.title,
                // This is precisely the number of lines that fill the height of the screen
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
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
}

@Preview(widthDp = 800, heightDp = 720)
@Composable
fun AllConversationsListPreview() {
    val conversation1 =
        Conversation(title = "What is the current weather in a very long query with many words and many lines this last bit this should be cut off")
    val conversation2 = Conversation(title = "What is 2 + 2")

    AllConversationsList(
        conversations = listOf(conversation1, conversation2),
    )
}


@Preview(widthDp = 800, heightDp = 720)
@Composable
fun ConversationTitleCardPreview() {
    val conversation = Conversation(title = "What is the current weather")

    Column {
        ConversationTitleCard(
            conversation = conversation,
        )
    }
}
