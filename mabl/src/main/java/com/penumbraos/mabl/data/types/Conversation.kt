package com.penumbraos.mabl.data.types

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActivity: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

@Serializable
data class ConversationWithMessages(
    @Embedded val conversation: Conversation,
    @Relation(
        parentColumn = "id",
        entityColumn = "conversationId"
    )
    val messages: List<ConversationMessage> = emptyList()
)
