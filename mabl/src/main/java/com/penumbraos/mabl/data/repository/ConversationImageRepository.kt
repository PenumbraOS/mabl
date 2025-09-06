package com.penumbraos.mabl.data.repository

import android.content.Context
import com.penumbraos.mabl.data.ImageFileManager
import com.penumbraos.mabl.data.dao.ConversationImageDao
import com.penumbraos.mabl.data.types.ConversationImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class ConversationImageRepository(
    context: Context,
    private val conversationImageDao: ConversationImageDao
) {
    private val imageFileManager = ImageFileManager(context)

    suspend fun saveImage(
        messageId: Long,
        byteArray: ByteArray,
        mimeType: String = "image/png"
    ): Pair<ConversationImage, File>? = withContext(Dispatchers.IO) {
        val savedImageInfo = imageFileManager.saveImage(byteArray, messageId, mimeType)
            ?: return@withContext null

        val conversationImage = ConversationImage(
            messageId = messageId,
            fileName = savedImageInfo.fileName,
            mimeType = savedImageInfo.mimeType,
            fileSizeBytes = savedImageInfo.fileSizeBytes,
            width = savedImageInfo.width,
            height = savedImageInfo.height
        )

        val id = conversationImageDao.insertImage(conversationImage)
        // TODO: Improve this return type
        Pair(conversationImage.copy(id = id), savedImageInfo.file)
    }

    suspend fun getImagesForMessage(messageId: Long): List<ConversationImage> {
        return conversationImageDao.getImagesForMessage(messageId)
    }

    fun getImagesForMessageFlow(messageId: Long): Flow<List<ConversationImage>> {
        return conversationImageDao.getImagesForMessageFlow(messageId)
    }

    suspend fun getImagesForConversation(conversationId: String): List<ConversationImage> {
        return conversationImageDao.getImagesForConversation(conversationId)
    }

    fun getImagesForConversationFlow(conversationId: String): Flow<List<ConversationImage>> {
        return conversationImageDao.getImagesForConversationFlow(conversationId)
    }

    suspend fun getImage(id: Long): ConversationImage? {
        return conversationImageDao.getImage(id)
    }

    suspend fun getImageByFileName(fileName: String): ConversationImage? {
        return conversationImageDao.getImageByFileName(fileName)
    }

    fun getImageFile(fileName: String) = imageFileManager.getImageFile(fileName)

    suspend fun deleteImage(id: Long): Boolean = withContext(Dispatchers.IO) {
        val image = conversationImageDao.getImage(id) ?: return@withContext false

        val fileDeleted = imageFileManager.deleteImage(image.fileName)
        if (fileDeleted) {
            conversationImageDao.deleteImageById(id)
        }
        fileDeleted
    }

    suspend fun deleteImagesForMessage(messageId: Long): Boolean = withContext(Dispatchers.IO) {
        val images = conversationImageDao.getImagesForMessage(messageId)
        val filesDeleted = imageFileManager.deleteImagesForConversation(images)

        if (filesDeleted) {
            conversationImageDao.deleteImagesForMessage(messageId)
        }
        filesDeleted
    }

    suspend fun deleteImagesForConversation(conversationId: String): Boolean =
        withContext(Dispatchers.IO) {
            val images = conversationImageDao.getImagesForConversation(conversationId)
            val filesDeleted = imageFileManager.deleteImagesForConversation(images)

            if (filesDeleted) {
                conversationImageDao.deleteImagesForConversation(conversationId)
            }
            filesDeleted
        }

    suspend fun getImageCountForConversation(conversationId: String): Int {
        return conversationImageDao.getImageCountForConversation(conversationId)
    }

    suspend fun getTotalImageSizeForConversation(conversationId: String): Long {
        return conversationImageDao.getTotalImageSizeForConversation(conversationId) ?: 0L
    }

    suspend fun getImageCountForMessage(messageId: Long): Int {
        return conversationImageDao.getImageCountForMessage(messageId)
    }
}