package com.penumbraos.mabl.data

import kotlinx.coroutines.flow.Flow

class MessageRepository(private val messageDao: MessageDao) {
    
    fun getAllMessages(): Flow<List<Message>> = messageDao.getAllMessages()
    
    suspend fun addMessage(content: String, isUser: Boolean) {
        messageDao.insertMessage(
            Message(
                content = content,
                isUser = isUser
            )
        )
    }
    
    suspend fun clearAllMessages() {
        messageDao.clearAll()
    }
}