package com.aibridge.chat.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.aibridge.chat.data.database.entities.ChatSessionEntity
import com.aibridge.chat.data.database.entities.MessageEntity
import com.aibridge.chat.data.database.entities.ApiKeyEntity
import com.aibridge.chat.data.database.entities.PortalConfigEntity
import com.aibridge.chat.data.database.entities.PortalDetailEntity
import com.aibridge.chat.data.database.entities.PortalUsageHistoryEntity
import com.aibridge.chat.data.database.dao.ChatSessionDao
import com.aibridge.chat.data.database.dao.MessageDao
import com.aibridge.chat.data.database.dao.ApiKeyDao
import com.aibridge.chat.data.database.dao.PortalConfigDao
import com.aibridge.chat.data.database.dao.PortalDetailDao
import com.aibridge.chat.data.database.dao.PortalUsageHistoryDao

@Database(
    entities = [
        ChatSessionEntity::class,
        MessageEntity::class,
        ApiKeyEntity::class,
        PortalConfigEntity::class,
        PortalDetailEntity::class,
        PortalUsageHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun messageDao(): MessageDao
    abstract fun apiKeyDao(): ApiKeyDao
    abstract fun portalConfigDao(): PortalConfigDao
    abstract fun portalDetailDao(): PortalDetailDao
    abstract fun portalUsageHistoryDao(): PortalUsageHistoryDao
    
    companion object {
        const val DATABASE_NAME = "ai_chat_database"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
