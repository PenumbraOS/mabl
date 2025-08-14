package com.penumbraos.mabl.interaction

import android.util.Log
import com.penumbraos.mabl.conversation.ConversationManager
import com.penumbraos.mabl.data.ConversationRepository
import com.penumbraos.mabl.services.AllControllers
import kotlinx.coroutines.flow.first

private const val TAG = "ConversationSessionManager"

class ConversationSessionManager(
    private val allControllers: AllControllers,
    private val conversationRepository: ConversationRepository
) : IConversationSessionManager {

    private var currentConversationManager: ConversationManager? = null
    private var currentConversationId: String? = null

    override suspend fun startNewConversation(): ConversationManager {
        Log.d(TAG, "Starting new conversation")

        val conversation = conversationRepository.createNewConversation()
        currentConversationId = conversation.id

        currentConversationManager = ConversationManager(
            allControllers = allControllers,
            conversationRepository = conversationRepository
        )
        currentConversationManager?.setConversationId(conversation.id)

        Log.d(TAG, "Created new conversation: ${conversation.id}")
        return currentConversationManager!!
    }

    override suspend fun resumeConversation(conversationId: String): ConversationManager? {
        Log.d(TAG, "Resuming conversation: $conversationId")

        val conversation = conversationRepository.getConversation(conversationId)
        if (conversation == null) {
            Log.w(TAG, "Conversation not found: $conversationId")
            return null
        }

        currentConversationId = conversationId
        currentConversationManager = ConversationManager(
            allControllers = allControllers,
            conversationRepository = conversationRepository
        )

        currentConversationManager?.resumeConversation(conversationId)
        conversationRepository.updateLastActivity(conversationId)

        Log.d(TAG, "Resumed conversation: $conversationId")
        return currentConversationManager
    }

    override suspend fun resumeLastConversation(): ConversationManager? {
        Log.d(TAG, "Resuming last conversation")

        val lastConversation = conversationRepository.getLastActiveConversation()
        return if (lastConversation != null) {
            resumeConversation(lastConversation.id)
        } else {
            Log.d(TAG, "No previous conversation found, starting new one")
            startNewConversation()
            currentConversationManager
        }
    }

    override suspend fun getCurrentConversation(): ConversationManager? {
        return currentConversationManager
    }

    override suspend fun getCurrentConversationId(): String? {
        return currentConversationId
    }

    override suspend fun getAllConversations(): List<ConversationSummary> {
        return try {
            val conversations = mutableListOf<ConversationSummary>()

            // Get all conversations from repository (get first emission)
            val conversationList = conversationRepository.getAllConversations().first()

            for (conversation in conversationList) {
                val summary = conversationRepository.getConversationSummary(conversation.id)
                if (summary != null) {
                    conversations.add(summary)
                }
            }

            conversations.sortedByDescending { it.lastActivity }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get conversations: ${e.message}")
            emptyList()
        }
    }

    override suspend fun deleteConversation(conversationId: String) {
        Log.d(TAG, "Deleting conversation: $conversationId")

        try {
            conversationRepository.deleteConversation(conversationId)

            // If this was the current conversation, clear it
            if (currentConversationId == conversationId) {
                currentConversationId = null
                currentConversationManager = null
                Log.d(TAG, "Cleared current conversation after deletion")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete conversation $conversationId: ${e.message}")
        }
    }

    override suspend fun updateConversationTitle(conversationId: String, title: String) {
        Log.d(TAG, "Updating conversation title: $conversationId -> $title")

        try {
            conversationRepository.updateConversationTitle(conversationId, title)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update conversation title: ${e.message}")
        }
    }

    suspend fun autoGenerateTitle(conversationId: String) {
        try {
            val generatedTitle = conversationRepository.generateConversationTitle(conversationId)
            updateConversationTitle(conversationId, generatedTitle)
            Log.d(TAG, "Auto-generated title for $conversationId: $generatedTitle")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to auto-generate title for $conversationId: ${e.message}")
        }
    }
}