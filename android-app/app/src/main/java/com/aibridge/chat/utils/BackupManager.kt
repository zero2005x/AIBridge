package com.aibridge.chat.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.aibridge.chat.data.database.dao.ChatSessionDao
import com.aibridge.chat.data.database.dao.MessageDao
import com.aibridge.chat.data.database.dao.ApiKeyDao
import com.aibridge.chat.domain.model.ChatSession
import com.aibridge.chat.domain.model.Message
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 備份和還原管理器
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatSessionDao: ChatSessionDao,
    private val messageDao: MessageDao,
    private val apiKeyDao: ApiKeyDao,
    private val gson: Gson
) {
    
    companion object {
        private const val TAG = "BackupManager"
        private const val BACKUP_VERSION = 1
        private const val BACKUP_FILE_PREFIX = "aibridge_backup"
        private const val BACKUP_FILE_EXTENSION = ".json"
    }
    
    /**
     * 備份資料結構
     */
    data class BackupData(
        val version: Int,
        val timestamp: Long,
        val appVersion: String,
        val sessions: List<BackupSession>,
        val messages: List<BackupMessage>,
        val statistics: BackupStatistics
    )
    
    data class BackupSession(
        val id: String,
        val title: String,
        val service: String,
        val model: String,
        val createdAt: Long,
        val lastModified: Long,
        val systemPrompt: String?,
        val messageCount: Int
    )
    
    data class BackupMessage(
        val id: String,
        val sessionId: String,
        val content: String,
        val isUser: Boolean,
        val timestamp: Long,
        val tokens: Int?,
        val model: String?,
        val service: String?
    )
    
    data class BackupStatistics(
        val totalSessions: Int,
        val totalMessages: Int,
        val backupSize: Long,
        val earliestMessage: Long?,
        val latestMessage: Long?
    )
    
    /**
     * 創建完整備份
     */
    suspend fun createBackup(): Result<BackupData> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Creating backup...")
            
            // 獲取所有活動會話
            val sessionEntities = chatSessionDao.getAllActiveSessions().first()
            val sessions = sessionEntities.map { entity ->
                BackupSession(
                    id = entity.id,
                    title = entity.title,
                    service = entity.service,
                    model = entity.model,
                    createdAt = entity.createdAt,
                    lastModified = entity.lastModified,
                    systemPrompt = entity.systemPrompt,
                    messageCount = messageDao.getMessageCount(entity.id)
                )
            }
            
            // 獲取所有訊息
            val allMessages = mutableListOf<BackupMessage>()
            for (session in sessions) {
                val sessionMessages = messageDao.getMessagesBySession(session.id).first()
                allMessages.addAll(sessionMessages.map { message ->
                    BackupMessage(
                        id = message.id,
                        sessionId = message.sessionId,
                        content = message.content,
                        isUser = message.isUser,
                        timestamp = message.timestamp,
                        tokens = message.tokens,
                        model = message.model,
                        service = message.service
                    )
                })
            }
            
            // 計算統計資料
            val statistics = BackupStatistics(
                totalSessions = sessions.size,
                totalMessages = allMessages.size,
                backupSize = 0L, // 稍後計算
                earliestMessage = allMessages.minOfOrNull { it.timestamp },
                latestMessage = allMessages.maxOfOrNull { it.timestamp }
            )
            
            val backupData = BackupData(
                version = BACKUP_VERSION,
                timestamp = System.currentTimeMillis(),
                appVersion = getAppVersion(),
                sessions = sessions,
                messages = allMessages,
                statistics = statistics
            )
            
            Log.i(TAG, "Backup created: ${sessions.size} sessions, ${allMessages.size} messages")
            Result.success(backupData)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create backup: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 將備份資料導出為 JSON 文件
     */
    suspend fun exportBackupToJson(backupData: BackupData, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Exporting backup to: $uri")
            
            val prettyGson = GsonBuilder()
                .setPrettyPrinting()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create()
            
            val jsonString = prettyGson.toJson(backupData)
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
                outputStream.flush()
            } ?: return@withContext Result.failure(IOException("無法開啟輸出流"))
            
            val fileSizeKB = jsonString.toByteArray().size / 1024
            Log.i(TAG, "Backup exported successfully, size: ${fileSizeKB}KB")
            
            Result.success("備份匯出成功，檔案大小：${fileSizeKB}KB")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export backup: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 從 JSON 文件匯入備份資料
     */
    suspend fun importBackupFromJson(uri: Uri): Result<BackupData> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Importing backup from: $uri")
            
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes().toString(Charsets.UTF_8)
            } ?: return@withContext Result.failure(IOException("無法讀取檔案"))
            
            val backupData = gson.fromJson(jsonString, BackupData::class.java)
            
            // 驗證備份格式
            if (backupData.version > BACKUP_VERSION) {
                return@withContext Result.failure(
                    IllegalArgumentException("備份檔案版本過新，請更新應用程式")
                )
            }
            
            Log.i(TAG, "Backup imported: ${backupData.sessions.size} sessions, ${backupData.messages.size} messages")
            Result.success(backupData)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import backup: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 還原備份資料到資料庫
     */
    suspend fun restoreBackup(
        backupData: BackupData,
        overwriteExisting: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Restoring backup...")
            
            if (overwriteExisting) {
                // 清除現有資料
                Log.d(TAG, "Clearing existing data...")
                val existingSessions = chatSessionDao.getAllActiveSessions().first()
                for (session in existingSessions) {
                    chatSessionDao.deactivateSession(session.id)
                    messageDao.deleteMessagesBySession(session.id)
                }
            }
            
            // 還原會話
            var sessionsRestored = 0
            for (session in backupData.sessions) {
                try {
                    val entity = com.aibridge.chat.data.database.entities.ChatSessionEntity(
                        id = session.id,
                        title = session.title,
                        service = session.service,
                        model = session.model,
                        createdAt = session.createdAt,
                        lastModified = session.lastModified,
                        systemPrompt = session.systemPrompt
                    )
                    chatSessionDao.insertSession(entity)
                    sessionsRestored++
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to restore session ${session.id}: ${e.message}")
                }
            }
            
            // 還原訊息
            var messagesRestored = 0
            for (message in backupData.messages) {
                try {
                    val entity = com.aibridge.chat.data.database.entities.MessageEntity(
                        id = message.id,
                        sessionId = message.sessionId,
                        content = message.content,
                        isUser = message.isUser,
                        timestamp = message.timestamp,
                        tokens = message.tokens,
                        model = message.model,
                        service = message.service
                    )
                    messageDao.insertMessage(entity)
                    messagesRestored++
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to restore message ${message.id}: ${e.message}")
                }
            }
            
            val resultMessage = "還原完成：$sessionsRestored 個對話，$messagesRestored 則訊息"
            Log.i(TAG, resultMessage)
            
            Result.success(resultMessage)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore backup: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 生成備份檔案名稱
     */
    fun generateBackupFileName(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        return "${BACKUP_FILE_PREFIX}_${timestamp}${BACKUP_FILE_EXTENSION}"
    }
    
    /**
     * 獲取分享備份的 Intent
     */
    fun getShareBackupIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "AI Chat 備份檔案")
            putExtra(Intent.EXTRA_TEXT, "分享 AI Chat 聊天記錄備份檔案")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    
    /**
     * 獲取應用版本
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    /**
     * 驗證備份檔案完整性
     */
    fun validateBackupData(backupData: BackupData): List<String> {
        val issues = mutableListOf<String>()
        
        // 檢查版本相容性
        if (backupData.version > BACKUP_VERSION) {
            issues.add("備份檔案版本過新")
        }
        
        // 檢查資料完整性
        val sessionIds = backupData.sessions.map { it.id }.toSet()
        val messageSessionIds = backupData.messages.map { it.sessionId }.toSet()
        
        val orphanMessages = messageSessionIds - sessionIds
        if (orphanMessages.isNotEmpty()) {
            issues.add("發現 ${orphanMessages.size} 則孤立訊息")
        }
        
        // 檢查統計資料
        if (backupData.statistics.totalSessions != backupData.sessions.size) {
            issues.add("會話數量統計不符")
        }
        
        if (backupData.statistics.totalMessages != backupData.messages.size) {
            issues.add("訊息數量統計不符")
        }
        
        return issues
    }
}
