package com.penumbraos.mabl.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<Message>>
    
    @Insert
    suspend fun insertMessage(message: Message)
    
    @Query("DELETE FROM messages")
    suspend fun clearAll()
}