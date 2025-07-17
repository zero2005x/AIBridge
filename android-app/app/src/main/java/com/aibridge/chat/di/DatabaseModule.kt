package com.aibridge.chat.di

import android.content.Context
import androidx.room.Room
import com.aibridge.chat.data.database.AppDatabase
import com.aibridge.chat.data.database.dao.ApiKeyDao
import com.aibridge.chat.data.database.dao.ChatSessionDao
import com.aibridge.chat.data.database.dao.MessageDao
import com.aibridge.chat.utils.BackupManager
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()
    }
    
    @Provides
    fun provideChatSessionDao(database: AppDatabase): ChatSessionDao = database.chatSessionDao()
    
    @Provides
    fun provideMessageDao(database: AppDatabase): MessageDao = database.messageDao()
    
    @Provides
    fun provideApiKeyDao(database: AppDatabase): ApiKeyDao = database.apiKeyDao()
    
    @Provides
    fun providePortalConfigDao(database: AppDatabase): com.aibridge.chat.data.database.dao.PortalConfigDao = database.portalConfigDao()
    
    @Provides
    fun providePortalDetailDao(database: AppDatabase): com.aibridge.chat.data.database.dao.PortalDetailDao = database.portalDetailDao()
    
    @Provides
    fun providePortalUsageHistoryDao(database: AppDatabase): com.aibridge.chat.data.database.dao.PortalUsageHistoryDao = database.portalUsageHistoryDao()
    
    @Provides
    @Singleton
    fun provideBackupManager(
        @ApplicationContext context: Context,
        chatSessionDao: ChatSessionDao,
        messageDao: MessageDao,
        apiKeyDao: ApiKeyDao,
        gson: Gson
    ): BackupManager {
        return BackupManager(context, chatSessionDao, messageDao, apiKeyDao, gson)
    }
}
