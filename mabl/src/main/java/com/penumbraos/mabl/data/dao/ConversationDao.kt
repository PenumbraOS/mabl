package com.penumbraos.mabl.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.penumbraos.mabl.data.types.Conversation
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY lastActivity DESC LIMIT :limit")
    fun getAllConversations(limit: Int = 50): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversation(id: String): Conversation?

    @Query("SELECT * FROM conversations ORDER BY lastActivity DESC LIMIT 1")
    fun getLastActiveConversation(): Flow<Conversation?>

    @Insert
    suspend fun insertConversation(conversation: Conversation)

    @Update
    suspend fun updateConversation(conversation: Conversation)

    @Query("UPDATE conversations SET lastActivity = :timestamp WHERE id = :id")
    suspend fun updateLastActivity(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE conversations SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: String, title: String)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)

    @Query("SELECT COUNT(*) FROM conversation_messages WHERE conversationId = :conversationId")
    suspend fun getMessageCount(conversationId: String): Int

    @Query(
        """
        SELECT cm.content FROM conversation_messages cm 
        WHERE cm.conversationId = :conversationId AND cm.type = 'user'
        ORDER BY cm.timestamp ASC LIMIT 1
    """
    )
    suspend fun getFirstUserMessage(conversationId: String): String?
}