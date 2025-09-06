package com.penumbraos.mabl.aipincore.view.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.penumbraos.mabl.aipincore.ConversationDisplay
import com.penumbraos.mabl.aipincore.view.model.PlatformViewModel
import com.penumbraos.mabl.data.repository.MessageRepository

@Composable
fun Conversations() {
    val viewModel = viewModel<PlatformViewModel>()
    val repository = remember { MessageRepository(viewModel.database.messageDao()) }
    val messages = repository.getAllMessages(count = 10).collectAsState(initial = emptyList())

    ConversationDisplay(messages = messages.value)
}