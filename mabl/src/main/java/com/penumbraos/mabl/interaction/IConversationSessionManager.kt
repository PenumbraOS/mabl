package com.penumbraos.mabl.interaction

import com.penumbraos.mabl.conversation.ConversationManager

interface IConversationSessionManager {
    suspend fun startNewConversation(): ConversationManager
    suspend fun resumeConversation(conversationId: String): ConversationManager?
    suspend fun resumeLastConversation(): ConversationManager?
    suspend fun getCurrentConversation(): ConversationManager?
    suspend fun getCurrentConversationId(): String?
    suspend fun getAllConversations(): List<ConversationSummary>
    suspend fun deleteConversation(conversationId: String)
    suspend fun updateConversationTitle(conversationId: String, title: String)
}

data class ConversationSummary(
    val id: String,
    val title: String,
    val lastActivity: Long,
    val messageCount: Int,
    val previewText: String
)