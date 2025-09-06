package com.penumbraos.mabl.data

import android.content.Context
import android.graphics.BitmapFactory
import com.penumbraos.mabl.data.types.ConversationImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

class ImageFileManager(private val context: Context) {

    private val imagesDir: File by lazy {
        File(context.filesDir, "conversation_images").also { dir ->
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
    }

    suspend fun saveImage(
        byteArray: ByteArray,
        messageId: Long,
        mimeType: String = "image/png"
    ): SavedImageInfo? = withContext(Dispatchers.IO) {
        try {
            val extension = getExtensionFromMimeType(mimeType)
            val fileName = "msg_${messageId}_${UUID.randomUUID()}$extension"
            val file = File(imagesDir, fileName)

            FileOutputStream(file).use { outputStream ->
                outputStream.write(byteArray)
            }

            val dimensions = getImageDimensions(file)

            SavedImageInfo(
                file = file,
                fileName = fileName,
                mimeType = mimeType,
                fileSizeBytes = file.length(),
                width = dimensions?.first,
                height = dimensions?.second
            )
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun getImageFile(fileName: String): File {
        return File(imagesDir, fileName)
    }

    suspend fun deleteImage(fileName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(imagesDir, fileName)
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteImagesForConversation(images: List<ConversationImage>): Boolean =
        withContext(Dispatchers.IO) {
            var allDeleted = true
            images.forEach { image ->
                try {
                    val file = File(imagesDir, image.fileName)
                    if (file.exists() && !file.delete()) {
                        allDeleted = false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    allDeleted = false
                }
            }
            allDeleted
        }

    suspend fun cleanupOrphanedFiles(validFileNames: Set<String>): Int =
        withContext(Dispatchers.IO) {
            var deletedCount = 0
            try {
                imagesDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.name !in validFileNames) {
                        if (file.delete()) {
                            deletedCount++
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            deletedCount
        }

    private fun getImageDimensions(file: File): Pair<Int, Int>? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            FileInputStream(file).use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
            if (options.outWidth > 0 && options.outHeight > 0) {
                Pair(options.outWidth, options.outHeight)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getExtensionFromMimeType(mimeType: String): String {
        return when (mimeType) {
            "image/jpeg" -> ".jpg"
            "image/png" -> ".png"
            "image/gif" -> ".gif"
            "image/webp" -> ".webp"
            "image/bmp" -> ".bmp"
            else -> ".jpg"
        }
    }

    data class SavedImageInfo(
        val file: File,
        val fileName: String,
        val mimeType: String,
        val fileSizeBytes: Long,
        val width: Int?,
        val height: Int?
    )
}