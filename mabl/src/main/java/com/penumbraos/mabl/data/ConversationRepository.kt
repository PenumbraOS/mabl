package com.penumbraos.mabl.data

import com.penumbraos.mabl.interaction.ConversationSummary
import kotlinx.coroutines.flow.Flow

class ConversationRepository(
    private val conversationDao: ConversationDao,
    private val conversationMessageDao: ConversationMessageDao
) {
    
    fun getAllConversations(): Flow<List<Conversation>> = conversationDao.getAllConversations()
    
    suspend fun getConversation(id: String): Conversation? = conversationDao.getConversation(id)
    
    suspend fun getLastActiveConversation(): Conversation? = conversationDao.getLastActiveConversation()
    
    suspend fun createNewConversation(title: String = "New Conversation"): Conversation {
        val conversation = Conversation(title = title)
        conversationDao.insertConversation(conversation)
        return conversation
    }
    
    suspend fun updateLastActivity(conversationId: String) {
        conversationDao.updateLastActivity(conversationId)
    }
    
    suspend fun updateConversationTitle(conversationId: String, title: String) {
        conversationDao.updateTitle(conversationId, title)
    }
    
    suspend fun deleteConversation(conversationId: String) {
        conversationDao.deleteConversation(conversationId)
    }
    
    suspend fun getConversationMessages(conversationId: String): List<ConversationMessage> {
        return conversationMessageDao.getConversationMessages(conversationId)
    }
    
    fun getConversationMessagesFlow(conversationId: String): Flow<List<ConversationMessage>> {
        return conversationMessageDao.getConversationMessagesFlow(conversationId)
    }
    
    suspend fun addMessage(
        conversationId: String,
        type: String,
        content: String,
        toolCalls: String? = null,
        toolCallId: String? = null
    ): ConversationMessage {
        val message = ConversationMessage(
            conversationId = conversationId,
            type = type,
            content = content,
            toolCalls = toolCalls,
            toolCallId = toolCallId
        )
        val id = conversationMessageDao.insertMessage(message)
        conversationDao.updateLastActivity(conversationId)
        return message.copy(id = id)
    }
    
    suspend fun getConversationSummaries(): List<ConversationSummary> {
        val conversations = conversationDao.getAllConversations()
        // Convert Flow to List for this method - in real usage you'd probably want Flow
        return emptyList() // TODO: Implement properly with Flow collection
    }
    
    suspend fun getConversationSummary(conversationId: String): ConversationSummary? {
        val conversation = conversationDao.getConversation(conversationId) ?: return null
        val messageCount = conversationDao.getMessageCount(conversationId)
        val previewText = conversationMessageDao.getLastMessageContent(conversationId) ?: ""
        
        return ConversationSummary(
            id = conversation.id,
            title = conversation.title,
            lastActivity = conversation.lastActivity,
            messageCount = messageCount,
            previewText = previewText.take(100) // Truncate to 100 chars
        )
    }
    
    suspend fun generateConversationTitle(conversationId: String): String {
        val firstMessage = conversationDao.getFirstUserMessage(conversationId)
        return if (firstMessage != null && firstMessage.length > 5) {
            firstMessage.take(50).trim() + if (firstMessage.length > 50) "..." else ""
        } else {
            "Conversation ${conversationId.take(8)}"
        }
    }
}