package com.penumbraos.mabl.aipincore.server.types

import com.penumbraos.mabl.data.ConversationMessage
import kotlinx.serialization.Serializable

@Serializable
data class ConversationWithMessages(
    val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActivity: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,

    val messages: List<ConversationMessage>
)
