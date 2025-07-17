package com.aibridge.chat.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aibridge.chat.R
import com.aibridge.chat.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通知管理器
 */
@Singleton
class NotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val CHANNEL_ID_GENERAL = "ai_chat_general"
        private const val CHANNEL_ID_BACKUP = "ai_chat_backup"
        private const val CHANNEL_ID_ERROR = "ai_chat_error"
        
        private const val NOTIFICATION_ID_BACKUP_COMPLETE = 1001
        private const val NOTIFICATION_ID_BACKUP_FAILED = 1002
        private const val NOTIFICATION_ID_API_KEY_EXPIRED = 1003
        private const val NOTIFICATION_ID_LONG_RESPONSE = 1004
    }
    
    init {
        createNotificationChannels()
    }
    
    /**
     * 創建通知頻道
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // 一般通知頻道
            val generalChannel = NotificationChannel(
                CHANNEL_ID_GENERAL,
                "一般通知",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "應用的一般通知"
            }
            
            // 備份通知頻道
            val backupChannel = NotificationChannel(
                CHANNEL_ID_BACKUP,
                "備份通知",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "備份相關通知"
            }
            
            // 錯誤通知頻道
            val errorChannel = NotificationChannel(
                CHANNEL_ID_ERROR,
                "錯誤通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "重要錯誤通知"
            }
            
            notificationManager.createNotificationChannels(
                listOf(generalChannel, backupChannel, errorChannel)
            )
        }
    }
    
    /**
     * 檢查通知權限
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }
    
    /**
     * 顯示備份完成通知
     */
    fun showBackupCompleteNotification(fileName: String, fileSize: String) {
        if (!hasNotificationPermission()) return
        
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BACKUP)
            .setSmallIcon(R.drawable.ic_notification) // 需要添加這個圖標
            .setContentTitle("備份完成")
            .setContentText("檔案：$fileName ($fileSize)")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("聊天記錄已成功備份\n檔案：$fileName\n大小：$fileSize")
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID_BACKUP_COMPLETE, notification)
    }
    
    /**
     * 顯示備份失敗通知
     */
    fun showBackupFailedNotification(error: String) {
        if (!hasNotificationPermission()) return
        
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ERROR)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("備份失敗")
            .setContentText(error)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("聊天記錄備份失敗：$error")
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID_BACKUP_FAILED, notification)
    }
    
    /**
     * 顯示 API Key 過期通知
     */
    fun showApiKeyExpiredNotification(serviceName: String) {
        if (!hasNotificationPermission()) return
        
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ERROR)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("API Key 過期")
            .setContentText("$serviceName 的 API Key 需要更新")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID_API_KEY_EXPIRED, notification)
    }
    
    /**
     * 顯示長時間等待 AI 回應的通知
     */
    fun showLongResponseNotification(sessionTitle: String) {
        if (!hasNotificationPermission()) return
        
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_GENERAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("AI 正在思考中...")
            .setContentText("對話：$sessionTitle")
            .setOngoing(true) // 持續顯示，直到手動移除
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID_LONG_RESPONSE, notification)
    }
    
    /**
     * 移除長時間等待通知
     */
    fun removeLongResponseNotification() {
        NotificationManagerCompat.from(context)
            .cancel(NOTIFICATION_ID_LONG_RESPONSE)
    }
    
    /**
     * 清除所有通知
     */
    fun clearAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
