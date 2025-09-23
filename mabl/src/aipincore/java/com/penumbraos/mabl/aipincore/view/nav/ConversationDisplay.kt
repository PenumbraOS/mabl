package com.penumbraos.mabl.aipincore.view.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.open.pin.ui.PinTheme
import com.penumbraos.mabl.aipincore.ConversationList
import com.penumbraos.mabl.aipincore.view.model.PlatformViewModel

@Composable
fun ConversationDisplay(conversationId: String) {
    val viewModel = viewModel<PlatformViewModel>()
    val messages = viewModel.conversationRepository.getConversationMessagesFlow(conversationId)
        .collectAsState(emptyList())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = PinTheme.colors.background)
    ) {
        ConversationList(
            messages = messages.value,
            modifier = Modifier.fillMaxSize()
        )
    }
}