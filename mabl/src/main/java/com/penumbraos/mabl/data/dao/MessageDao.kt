package com.penumbraos.mabl.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.penumbraos.mabl.data.types.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp DESC LIMIT :count")
    fun getAllMessages(count: Int): Flow<List<Message>>

    @Insert
    suspend fun insertMessage(message: Message)

    @Query("DELETE FROM messages")
    suspend fun clearAll()
}