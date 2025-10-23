package com.penumbraos.mabl.data.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.penumbraos.mabl.data.types.Conversation
import kotlinx.coroutines.flow.Flow

data class ConversationWithFirstUserMessage(
    @Embedded val conversation: Conversation,
    val firstUserMessage: String?
)

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY lastActivity DESC LIMIT :limit")
    fun getAllConversations(limit: Int = 50): Flow<List<Conversation>>

    @Query(
        """
        WITH first_user_messages AS (
            SELECT 
                cm.conversationId,
                cm.content
            FROM conversation_messages cm
            INNER JOIN (
                SELECT 
                    conversationId,
                    MIN(timestamp) AS firstTimestamp,
                    MIN(id) AS firstId
                FROM conversation_messages
                WHERE type = 'user'
                GROUP BY conversationId
            ) first_user_timestamp ON first_user_timestamp.conversationId = cm.conversationId
            WHERE 
                cm.type = 'user' AND
                cm.timestamp = first_user_timestamp.firstTimestamp AND
                cm.id = first_user_timestamp.firstId
        )
        SELECT 
            c.id AS id,
            c.title AS title,
            c.createdAt AS createdAt,
            c.lastActivity AS lastActivity,
            c.isActive AS isActive,
            fum.content AS firstUserMessage
        FROM conversations c
        LEFT JOIN first_user_messages fum ON fum.conversationId = c.id
        ORDER BY c.lastActivity DESC
        LIMIT :limit
        """
    )
    fun getConversationsWithFirstUserMessage(limit: Int = 50): Flow<List<ConversationWithFirstUserMessage>>

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

}