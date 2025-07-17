package com.aibridge.chat.utils

import android.util.Log
import com.aibridge.chat.data.repository.AuthRepository
import com.aibridge.chat.data.repository.ChatRepository
import com.aibridge.chat.data.repository.LoginState
import com.aibridge.chat.domain.model.PortalCredentials
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatDebugHelper @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository
) {
    companion object {
        private const val TAG = "ChatDebugHelper"
    }

    suspend fun diagnoseChatIssue(): ChatDiagnosticResult {
        Log.i(TAG, "Running chat functionality diagnostics...")
        
        val results = mutableMapOf<String, Boolean>()
        val errors = mutableListOf<String>()
        val info = mutableListOf<String>()
        
        try {
            // 1. 檢查登入狀態
            val loginState = authRepository.loginState.first()
            val isLoggedIn = loginState is LoginState.LoggedIn
            results["user_logged_in"] = isLoggedIn
            
            if (isLoggedIn) {
                val loggedInState = loginState as LoginState.LoggedIn
                info.add("登入用戶: ${loggedInState.userInfo.username}")
                info.add("Session Cookie: ${loggedInState.sessionCookie.take(20)}...")
            } else {
                errors.add("用戶未登入")
            }
            
            // 2. 檢查存儲的憑證
            val credentials = authRepository.getStoredCredentials()
            val hasCredentials = credentials != null
            results["has_stored_credentials"] = hasCredentials
            
            if (hasCredentials && credentials != null) {
                info.add("存儲的憑證: ${credentials.username}@${credentials.baseUrl}")
                info.add("密碼長度: ${credentials.password.length}")
            } else {
                errors.add("無法獲取存儲的憑證")
            }
            
            // 3. 檢查聊天會話
            val sessions = chatRepository.getAllSessions().first()
            results["has_chat_sessions"] = sessions.isNotEmpty()
            
            if (sessions.isNotEmpty()) {
                info.add("聊天會話數量: ${sessions.size}")
                sessions.forEach { session ->
                    info.add("會話: ${session.title} (${session.service}/${session.model})")
                }
            } else {
                errors.add("沒有聊天會話，請先創建新對話")
            }
            
            // 4. 檢查網路連線
            // 這部分在實際使用時會由 NetworkUtils 處理
            
            val overallSuccess = isLoggedIn && hasCredentials
            
            return ChatDiagnosticResult(
                success = overallSuccess,
                results = results,
                errors = errors,
                info = info
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "診斷過程中發生錯誤: ${e.message}", e)
            errors.add("診斷失敗: ${e.message}")
            
            return ChatDiagnosticResult(
                success = false,
                results = results,
                errors = errors,
                info = info
            )
        }
    }
    
    suspend fun testChatFlow(sessionId: String): ChatTestResult {
        Log.i(TAG, "Testing chat flow for session: $sessionId")
        
        try {
            val credentials = authRepository.getStoredCredentials()
            if (credentials == null) {
                return ChatTestResult(
                    success = false,
                    message = "無法獲取登入憑證"
                )
            }
            
            val testMessage = "這是一個測試訊息，請回應確認 AI 服務正常工作。"
            
            val result = chatRepository.sendMessageToAI(
                sessionId = sessionId,
                message = testMessage,
                username = credentials.username,
                password = credentials.password,
                baseUrl = credentials.baseUrl
            )
            
            return if (result.isSuccess) {
                ChatTestResult(
                    success = true,
                    message = "聊天功能測試成功",
                    responseMessage = result.getOrNull()?.content
                )
            } else {
                ChatTestResult(
                    success = false,
                    message = "聊天功能測試失敗: ${result.exceptionOrNull()?.message}"
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "聊天流程測試失敗: ${e.message}", e)
            return ChatTestResult(
                success = false,
                message = "測試過程中發生錯誤: ${e.message}"
            )
        }
    }
}

data class ChatDiagnosticResult(
    val success: Boolean,
    val results: Map<String, Boolean>,
    val errors: List<String>,
    val info: List<String>
) {
    fun getSummary(): String {
        val builder = StringBuilder()
        builder.append("=== 聊天功能診斷報告 ===\n")
        builder.append("整體狀態: ${if (success) "正常" else "異常"}\n\n")
        
        builder.append("檢查結果:\n")
        results.forEach { (key, value) ->
            val status = if (value) "✓" else "✗"
            builder.append("  $status $key: $value\n")
        }
        
        if (info.isNotEmpty()) {
            builder.append("\n詳細資訊:\n")
            info.forEach { info ->
                builder.append("  • $info\n")
            }
        }
        
        if (errors.isNotEmpty()) {
            builder.append("\n發現問題:\n")
            errors.forEach { error ->
                builder.append("  ⚠ $error\n")
            }
        }
        
        return builder.toString()
    }
}

data class ChatTestResult(
    val success: Boolean,
    val message: String,
    val responseMessage: String? = null
)
