package com.penumbraos.mabl.services

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import kotlinx.serialization.Serializable

private const val TAG = "CameraRollService"

@Serializable
data class CameraRollImage(
    val id: Long,
    val fileName: String,
    val filePath: String,
    val mimeType: String,
    val dateAdded: Long,
    val dateTaken: Long,
    val width: Int,
    val height: Int,
    val size: Long
)

class CameraRollService(private val context: Context) {

    fun getCameraRollImages(limit: Int = 100, offset: Int = 0): List<CameraRollImage> {
        val images = mutableListOf<CameraRollImage>()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

                // Skip to offset position and collect up to limit items
                var currentIndex = 0
                var collected = 0
                while (cursor.moveToNext()) {
                    if (currentIndex < offset) {
                        currentIndex++
                        continue
                    }
                    
                    if (collected >= limit) {
                        break
                    }
                    val id = cursor.getLong(idColumn)
                    val fileName = cursor.getString(nameColumn) ?: "unknown"
                    val filePath = cursor.getString(dataColumn) ?: ""
                    val mimeType = cursor.getString(mimeTypeColumn) ?: "image/jpeg"
                    val dateAdded = cursor.getLong(dateAddedColumn) * 1000 // Convert to milliseconds
                    val dateTaken = cursor.getLong(dateTakenColumn).let { 
                        if (it > 0) it else dateAdded 
                    }
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val size = cursor.getLong(sizeColumn)

                    images.add(
                        CameraRollImage(
                            id = id,
                            fileName = fileName,
                            filePath = filePath,
                            mimeType = mimeType,
                            dateAdded = dateAdded,
                            dateTaken = dateTaken,
                            width = width,
                            height = height,
                            size = size
                        )
                    )
                    collected++
                    currentIndex++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying camera roll", e)
        }

        Log.d(TAG, "Retrieved ${images.size} images from camera roll")
        return images
    }

    fun getCameraRollImageById(imageId: Long): CameraRollImage? {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE
        )

        val selection = "${MediaStore.Images.Media._ID} = ?"
        val selectionArgs = arrayOf(imageId.toString())

        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                    val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                    val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                    val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                    val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

                    val id = cursor.getLong(idColumn)
                    val fileName = cursor.getString(nameColumn) ?: "unknown"
                    val filePath = cursor.getString(dataColumn) ?: ""
                    val mimeType = cursor.getString(mimeTypeColumn) ?: "image/jpeg"
                    val dateAdded = cursor.getLong(dateAddedColumn) * 1000
                    val dateTaken = cursor.getLong(dateTakenColumn).let { 
                        if (it > 0) it else dateAdded 
                    }
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val size = cursor.getLong(sizeColumn)

                    return CameraRollImage(
                        id = id,
                        fileName = fileName,
                        filePath = filePath,
                        mimeType = mimeType,
                        dateAdded = dateAdded,
                        dateTaken = dateTaken,
                        width = width,
                        height = height,
                        size = size
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying camera roll image by ID", e)
        }

        return null
    }
}