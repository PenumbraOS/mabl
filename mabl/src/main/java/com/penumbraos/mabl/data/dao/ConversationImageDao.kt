package com.penumbraos.mabl.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.penumbraos.mabl.data.types.ConversationImage
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationImageDao {
    @Query("SELECT * FROM conversation_images WHERE messageId = :messageId ORDER BY timestamp ASC")
    suspend fun getImagesForMessage(messageId: Long): List<ConversationImage>

    @Query("SELECT * FROM conversation_images WHERE messageId = :messageId ORDER BY timestamp ASC")
    fun getImagesForMessageFlow(messageId: Long): Flow<List<ConversationImage>>

    @Query("""SELECT ci.* FROM conversation_images ci 
             INNER JOIN conversation_messages cm ON ci.messageId = cm.id 
             WHERE cm.conversationId = :conversationId ORDER BY ci.timestamp ASC""")
    suspend fun getImagesForConversation(conversationId: String): List<ConversationImage>

    @Query("""SELECT ci.* FROM conversation_images ci 
             INNER JOIN conversation_messages cm ON ci.messageId = cm.id 
             WHERE cm.conversationId = :conversationId ORDER BY ci.timestamp ASC""")
    fun getImagesForConversationFlow(conversationId: String): Flow<List<ConversationImage>>

    @Query("SELECT * FROM conversation_images WHERE id = :id")
    suspend fun getImage(id: Long): ConversationImage?

    @Query("SELECT * FROM conversation_images WHERE fileName = :fileName")
    suspend fun getImageByFileName(fileName: String): ConversationImage?

    @Insert
    suspend fun insertImage(image: ConversationImage): Long

    @Delete
    suspend fun deleteImage(image: ConversationImage)

    @Query("DELETE FROM conversation_images WHERE id = :id")
    suspend fun deleteImageById(id: Long)

    @Query("DELETE FROM conversation_images WHERE messageId = :messageId")
    suspend fun deleteImagesForMessage(messageId: Long)

    @Query("""DELETE FROM conversation_images WHERE messageId IN 
             (SELECT id FROM conversation_messages WHERE conversationId = :conversationId)""")
    suspend fun deleteImagesForConversation(conversationId: String)

    @Query("""SELECT COUNT(*) FROM conversation_images ci 
             INNER JOIN conversation_messages cm ON ci.messageId = cm.id 
             WHERE cm.conversationId = :conversationId""")
    suspend fun getImageCountForConversation(conversationId: String): Int

    @Query("""SELECT SUM(fileSizeBytes) FROM conversation_images ci 
             INNER JOIN conversation_messages cm ON ci.messageId = cm.id 
             WHERE cm.conversationId = :conversationId""")
    suspend fun getTotalImageSizeForConversation(conversationId: String): Long?

    @Query("SELECT COUNT(*) FROM conversation_images WHERE messageId = :messageId")
    suspend fun getImageCountForMessage(messageId: Long): Int
}