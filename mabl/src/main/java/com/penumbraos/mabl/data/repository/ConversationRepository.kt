package com.penumbraos.mabl.data.repository

import com.penumbraos.mabl.data.dao.ConversationDao
import com.penumbraos.mabl.data.dao.ConversationMessageDao
import com.penumbraos.mabl.data.types.Conversation
import com.penumbraos.mabl.data.types.ConversationMessage
import kotlinx.coroutines.flow.Flow

class ConversationRepository(
    private val conversationDao: ConversationDao,
    private val conversationMessageDao: ConversationMessageDao,
    private val conversationImageRepository: ConversationImageRepository? = null
) {

    fun getAllConversations(limit: Int = 50): List<Conversation> =
        conversationDao.getAllConversations(limit)

    suspend fun getConversation(id: String): Conversation? = conversationDao.getConversation(id)

    suspend fun getLastActiveConversation(): Conversation? =
        conversationDao.getLastActiveConversation()

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
        conversationImageRepository?.deleteImagesForConversation(conversationId)
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
}