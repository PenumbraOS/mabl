package com.penumbraos.mabl.aipincore.view.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.penumbraos.mabl.aipincore.ConversationDisplay
import com.penumbraos.mabl.data.AppDatabase
import com.penumbraos.mabl.data.MessageRepository

@Composable
fun Home(database: AppDatabase) {
    val repository = remember { MessageRepository(database.messageDao()) }
    val messages = repository.getAllMessages().collectAsState(initial = emptyList())

    ConversationDisplay(messages = messages.value)
}