package com.penumbraos.mabl.aipincore.server.types

import com.penumbraos.mabl.data.types.ConversationImage
import kotlinx.serialization.Serializable

@Serializable
data class MessageWithImages(
    val id: Long,
    val conversationId: String,
    val type: String,
    val content: String,
    val toolCalls: String? = null,
    val toolCallId: String? = null,
    val timestamp: Long,
    val images: List<ConversationImage> = emptyList()
)

@Serializable
data class AugmentedConversation(
    val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActivity: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val messages: List<MessageWithImages>
)
