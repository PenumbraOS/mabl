package com.penumbraos.mabl.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "conversation_messages",
    foreignKeys = [ForeignKey(
        entity = Conversation::class,
        parentColumns = ["id"],
        childColumns = ["conversationId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ConversationMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: String,
    /**
     * "user", "assistant", "tool"
     */
    val type: String,
    val content: String,
    /**
     * JSON serialized tool calls
     */
    val toolCalls: String? = null,
    val toolCallId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)