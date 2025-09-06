package com.penumbraos.mabl.data.types

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "conversation_images",
    foreignKeys = [ForeignKey(
        entity = ConversationMessage::class,
        parentColumns = ["id"],
        childColumns = ["messageId"],
        onDelete = ForeignKey.Companion.CASCADE
    )]
)
data class ConversationImage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: Long,
    val fileName: String,
    val mimeType: String,
    val fileSizeBytes: Long,
    val width: Int? = null,
    val height: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)