package com.penumbraos.mabl.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.penumbraos.mabl.data.dao.ConversationDao
import com.penumbraos.mabl.data.dao.ConversationImageDao
import com.penumbraos.mabl.data.dao.ConversationMessageDao
import com.penumbraos.mabl.data.types.Conversation
import com.penumbraos.mabl.data.types.ConversationImage
import com.penumbraos.mabl.data.types.ConversationMessage

@Database(
    entities = [Conversation::class, ConversationMessage::class, ConversationImage::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun conversationMessageDao(): ConversationMessageDao
    abstract fun conversationImageDao(): ConversationImageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `conversations` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `lastActivity` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                database.execSQL("CREATE TABLE IF NOT EXISTS `conversation_messages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `conversationId` TEXT NOT NULL, `type` TEXT NOT NULL, `content` TEXT NOT NULL, `toolCalls` TEXT, `toolCallId` TEXT, `timestamp` INTEGER NOT NULL, FOREIGN KEY(`conversationId`) REFERENCES `conversations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `conversation_images` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `messageId` INTEGER NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        `fileSizeBytes` INTEGER NOT NULL,
                        `width` INTEGER,
                        `height` INTEGER,
                        `timestamp` INTEGER NOT NULL,
                        FOREIGN KEY(`messageId`) REFERENCES `conversation_messages`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}