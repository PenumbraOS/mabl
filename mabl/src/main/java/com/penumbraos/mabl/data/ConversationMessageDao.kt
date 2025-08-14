package com.penumbraos.mabl.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationMessageDao {
    @Query("SELECT * FROM conversation_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getConversationMessages(conversationId: String): List<ConversationMessage>
    
    @Query("SELECT * FROM conversation_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getConversationMessagesFlow(conversationId: String): Flow<List<ConversationMessage>>
    
    @Insert
    suspend fun insertMessage(message: ConversationMessage): Long
    
    @Query("DELETE FROM conversation_messages WHERE conversationId = :conversationId")
    suspend fun deleteConversationMessages(conversationId: String)
    
    @Query("""
        SELECT content FROM conversation_messages 
        WHERE conversationId = :conversationId AND type IN ('user', 'assistant')
        ORDER BY timestamp DESC LIMIT 1
    """)
    suspend fun getLastMessageContent(conversationId: String): String?
}