package com.penumbraos.mabl.data.types

import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.serialization.Serializable

@Serializable
data class ConversationMessageWithImages(
    @Embedded val message: ConversationMessage,
    @Relation(
        parentColumn = "id",
        entityColumn = "messageId"
    )
    val images: List<ConversationImage> = emptyList()
)

@Serializable
data class ConversationWithImagesData(
    @Embedded val conversation: Conversation,
    @Relation(
        entity = ConversationMessage::class,
        parentColumn = "id",
        entityColumn = "conversationId"
    )
    val messagesWithImages: List<ConversationMessageWithImages> = emptyList()
) {
    val allImages: List<ConversationImage>
        get() = messagesWithImages.flatMap { it.images }
}