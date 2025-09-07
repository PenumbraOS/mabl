package com.penumbraos.mabl.aipincore.view.nav

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.open.pin.ui.components.text.PinText
import com.penumbraos.mabl.aipincore.view.model.PlatformViewModel

@Composable
fun Conversations() {
    val viewModel = viewModel<PlatformViewModel>()
//    val repository = remember { MessageRepository(viewModel.database.messageDao()) }
//    val messages = repository.getAllMessages(count = 10).collectAsState(initial = emptyList())
//
//    ConversationDisplay(messages = messages.value)
    PinText("TODO")
}