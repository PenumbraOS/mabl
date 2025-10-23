package com.penumbraos.mabl.aipincore

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.open.pin.ui.PinTheme
import com.open.pin.ui.components.text.PinText
import com.open.pin.ui.components.views.ListView
import com.open.pin.ui.debug.VoronoiVisualizer
import com.open.pin.ui.theme.PinColors
import com.open.pin.ui.theme.PinFonts
import com.open.pin.ui.theme.PinTypography
import com.open.pin.ui.utils.PinDimensions
import com.open.pin.ui.utils.modifiers.ProvideSnapCoordinator
import com.open.pin.ui.utils.modifiers.SnapCoordinator
import com.penumbraos.mabl.aipincore.view.TouchInterceptor
import com.penumbraos.mabl.aipincore.view.model.ConversationDisplayNav
import com.penumbraos.mabl.aipincore.view.model.NavViewModel
import com.penumbraos.mabl.aipincore.view.model.PlatformViewModel
import com.penumbraos.mabl.aipincore.view.nav.Navigation
import com.penumbraos.mabl.data.types.ConversationMessage
import com.penumbraos.mabl.ui.UIComponents

@Composable
fun PlatformUI(uiComponents: UIComponents?) {
    val snapCoordinator = remember { mutableStateOf(SnapCoordinator()) }

    var displayDebugView = remember { mutableStateOf(false) }

    PinTheme {
        ProvideSnapCoordinator(coordinator = snapCoordinator.value) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = PinTheme.colors.background)
            ) {
                // For some very strange reason things on the bottom are higher z-index
                if (uiComponents != null) {
                    Content(uiComponents, displayDebugView)
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color = PinTheme.colors.background),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                if (displayDebugView.value) {
                    VoronoiVisualizer(
                        alpha = 0.4f
                    )
                }
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context -> TouchInterceptor(snapCoordinator.value, context) }
                )
            }
        }
    }
}

@Composable
fun Content(uiComponents: UIComponents, displayDebugView: MutableState<Boolean>) {
    val platformViewModel = uiComponents.platformCapabilities.getViewModel() as PlatformViewModel

    // Push view model into owner
    viewModel<PlatformViewModel>(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return platformViewModel as T
        }
    })
    viewModel<NavViewModel>(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return platformViewModel.navViewModel as T
        }
    })

    val processLifecycle = remember { ProcessLifecycleOwner.get().lifecycle }

    LifecycleStartEffect(processLifecycle) {
        platformViewModel.appIsForeground = true
        onStopOrDispose {
            platformViewModel.appIsForeground = false
        }
    }

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current

    LaunchedEffect(Unit) {
        platformViewModel.backGestureEvent.collect {
            if (!platformViewModel.navViewModel.isHomeScreen.value) {
                backDispatcher?.onBackPressedDispatcher?.onBackPressed()
            }
        }
    }

    LaunchedEffect(Unit) {
        platformViewModel.openCurrentConversationEvent.collect {
            val currentConversation =
                platformViewModel.conversationRepository.getLastActiveConversation()
            if (currentConversation != null) {
                platformViewModel.navViewModel.pushView(ConversationDisplayNav(currentConversation.id))
            }
        }
    }

    LaunchedEffect(Unit) {
        platformViewModel.debugChannel.collect {
            displayDebugView.value = it
        }
    }

    Navigation()
}

@Composable
fun ConversationList(
    modifier: Modifier = Modifier,
    navViewModel: NavViewModel = viewModel(),
    messages: List<ConversationMessage>,
) {
    val menuOpen by navViewModel.isMenuOpen

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
            modifier = modifier,
            showScrollButtons = !menuOpen,
            autoHideButtons = true
        ) {
            items(messages, key = { it.id }) { message ->
                MessageItem(
                    message = message,
                )
            }
        }
    }
}

@Composable
fun MessageItem(
    message: ConversationMessage,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        PinText(
            text = if (message.type == "user") "You" else "MABL",
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

@Preview(widthDp = 800, heightDp = 720)
@Composable
fun ConversationDisplayPreview() {
    val messages = listOf(
        ConversationMessage(1, "someConversation", "user", "Hello!"),
        ConversationMessage(
            2,
            "someConversation",
            "assistant",
            "Next week in Seoul, expect showers on Thursday morning with temperatures ranging from 25 to 30 degrees. It will be sunny the rest of the week.",
        )
    )

    val snapCoordinator = remember { mutableStateOf(SnapCoordinator()) }

    PinTheme {
        ProvideSnapCoordinator(coordinator = snapCoordinator.value) {
//            ConversationDisplay(messages = messages)
        }
    }
}