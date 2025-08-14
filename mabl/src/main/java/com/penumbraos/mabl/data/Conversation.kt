package com.penumbraos.mabl.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActivity: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)