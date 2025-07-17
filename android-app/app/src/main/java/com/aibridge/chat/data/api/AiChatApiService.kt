package com.aibridge.chat.data.api

import android.util.Log
import com.aibridge.chat.domain.model.BackendChatResponse
import com.aibridge.chat.config.ApiConfig
import com.aibridge.chat.data.repository.AuthRepository
import com.aibridge.chat.domain.model.PortalConfig
import com.aibridge.chat.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import java.net.UnknownHostException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.ConnectException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiChatApiService @Inject constructor(
    private val httpClient: OkHttpClient,
    private val networkUtils: NetworkUtils,
    private val gson: Gson,
    private val portalApiService: PortalApiService,
    private val authRepository: AuthRepository
) {
    companion object {
        private const val TAG = "AiChatApiService"
    }

    suspend fun sendChatMessage(
        username: String,
        password: String,
        baseUrl: String,
        message: String,
        portalConfig: PortalConfig? = null
    ): ChatResponse = withContext(Dispatchers.IO) {
        
        // 根據當前模式選擇使用的API
        val currentMode = ApiConfig.getCurrentMode()
        Log.d(TAG, "Current mode: $currentMode")
        
        return@withContext when (currentMode) {
            ApiConfig.RunMode.PRODUCTION -> {
                // 生產模式：使用Portal API
                Log.d(TAG, "Using Portal API for production mode")
                
                // 優先使用PortalConfig，如果沒有則使用默認邏輯
                val requestedPortalId = portalConfig?.id ?: authRepository.getDefaultPortalId() ?: ApiConfig.DEFAULT_PORTAL_ID
                val availablePortalIds = authRepository.getAvailablePortalIds()
                
                // 如果沒有可用的Portal ID，檢查是否應該回退到測試模式
                val workingPortalIds = if (availablePortalIds.isEmpty()) {
                    Log.w(TAG, "No Portal IDs with full access available")
                    
                    // 嘗試使用只讀Portal ID列表，但警告用戶可能遇到權限問題
                    val readOnlyIds = listOf("1", "2", "3", "4", "5", "10", "11", "12", "13")
                    Log.w(TAG, "Falling back to read-only Portal IDs: $readOnlyIds")
                    Log.w(TAG, "Note: These Portal IDs may only have GET access, POST requests might fail with 403")
                    
                    readOnlyIds
                } else {
                    availablePortalIds
                }
                
                // 驗證請求的 Portal ID 是否在工作列表中
                val portalId = if (workingPortalIds.contains(requestedPortalId)) {
                    Log.d(TAG, "Requested Portal ID $requestedPortalId is available")
                    requestedPortalId
                } else {
                    Log.w(TAG, "Requested Portal ID $requestedPortalId not available. Available IDs: $workingPortalIds")
                    // 使用第一個可用的 Portal ID 作為備選
                    val fallbackId = workingPortalIds.first()
                    Log.d(TAG, "Using fallback Portal ID: $fallbackId")
                    fallbackId
                }
                
                val alternativeIds = workingPortalIds.filter { it != portalId }
                
                // 使用PortalConfig中的參數
                val userPrompt = portalConfig?.parameters?.get("USERPROMPT")?.value ?: message
                val uploadedFile = portalConfig?.parameters?.get("USERUPLOADFILE")?.value
                
                Log.d(TAG, "Using Portal ID: $portalId")
                Log.d(TAG, "Portal Config: ${portalConfig?.name}")
                Log.d(TAG, "User Prompt: $userPrompt")
                Log.d(TAG, "Available Portal IDs: $workingPortalIds")
                
                // 從 AuthRepository 獲取當前的 session cookie
                val sessionCookie = authRepository.getCurrentSessionCookie()
                Log.d(TAG, "Session cookie available: ${sessionCookie != null}")
                
                // 首次嘗試發送聊天訊息
                var response = portalApiService.sendPortalChatMessage(
                    userPrompt = userPrompt,
                    uploadedFile = uploadedFile,
                    portalId = portalId,
                    alternativePortalIds = alternativeIds,
                    portalConfig = portalConfig,
                    sessionCookie = sessionCookie
                )
                
                // 如果 session 過期，嘗試重新認證並重試
                if (!response.success && response.errorDetails == "LOGIN_REQUIRED") {
                    Log.i(TAG, "Session expired, attempting to re-authenticate")
                    
                    // 重新認證 - 需要創建 PortalCredentials
                    val credentials = authRepository.getStoredCredentials()
                    if (credentials != null) {
                        val loginResult = authRepository.login(credentials)
                        if (loginResult.success) {
                            Log.i(TAG, "Re-authentication successful, retrying chat request")
                            
                            // 獲取新的 session cookie
                            val newSessionCookie = authRepository.getCurrentSessionCookie()
                            Log.d(TAG, "New session cookie available: ${newSessionCookie != null}")
                            
                            // 重新嘗試發送聊天訊息
                            response = portalApiService.sendPortalChatMessage(
                                userPrompt = userPrompt,
                                uploadedFile = uploadedFile,
                                portalId = portalId,
                                alternativePortalIds = alternativeIds,
                                portalConfig = portalConfig,
                                sessionCookie = newSessionCookie
                            )
                        } else {
                            Log.e(TAG, "Re-authentication failed: ${loginResult.message}")
                            return@withContext ChatResponse(
                                success = false,
                                message = "重新認證失敗：${loginResult.message}"
                            )
                        }
                    } else {
                        Log.e(TAG, "No stored credentials available for re-authentication")
                        return@withContext ChatResponse(
                            success = false,
                            message = "無法重新認證：未找到存儲的登入憑證"
                        )
                    }
                }
                
                // 如果 Portal ID 權限被拒絕，嘗試使用備選 Portal ID
                if (!response.success && response.errorDetails == "PORTAL_ACCESS_DENIED" && alternativeIds.isNotEmpty()) {
                    Log.i(TAG, "Portal access denied for ID $portalId, trying alternative Portal IDs: $alternativeIds")
                    
                    // 遞歸嘗試所有備選 Portal ID
                    response = tryAlternativePortalIds(
                        userPrompt = userPrompt,
                        uploadedFile = uploadedFile,
                        alternativeIds = alternativeIds,
                        portalConfig = portalConfig,
                        sessionCookie = sessionCookie
                    )
                }
                
                // 如果所有Portal ID都失敗且錯誤是權限相關，提供更好的用戶指導
                if (!response.success && (response.errorDetails == "PORTAL_ACCESS_DENIED" || response.errorDetails == "ALL_PORTALS_ACCESS_DENIED")) {
                    Log.w(TAG, "All Portal IDs failed due to permissions, considering fallback to test mode")
                    
                    // 檢查是否可以切換到測試模式作為臨時解決方案
                    if (currentMode == ApiConfig.RunMode.PRODUCTION) {
                        Log.i(TAG, "Production mode detected with permission issues, suggesting test mode fallback")
                        
                        response = ChatResponse(
                            success = false,
                            message = "❌ Portal權限不足\n\n" +
                                     "當前系統處於生產模式，但用戶賬號沒有任何Portal的提交權限。\n\n" +
                                     "🔧 解決方案：\n" +
                                     "1. **推薦**：聯繫系統管理員申請Portal提交權限\n" +
                                     "2. 確認用戶賬號是否有訪問特定Portal ID的權限\n" +
                                     "3. 暫時可以切換到測試模式驗證APP功能\n\n" +
                                     "💡 提示：如果您知道有可用的Portal ID，可以在Portal管理中手動配置。\n\n" +
                                     "🛠️ 開發者注意：如需測試模式，請修改ApiConfig.kt中的CURRENT_MODE為\"TEST\"",
                            errorDetails = "INSUFFICIENT_PORTAL_PERMISSIONS"
                        )
                    } else {
                        response = ChatResponse(
                            success = false,
                            message = "❌ Portal權限不足\n\n" +
                                     "當前用戶賬號沒有任何Portal的提交權限。\n\n" +
                                     "🔧 解決方案：\n" +
                                     "1. 聯繫系統管理員申請Portal提交權限\n" +
                                     "2. 確認是否有其他可用的Portal ID\n" +
                                     "3. 在Portal管理中手動配置可用的Portal\n\n" +
                                     "💡 提示：如果您知道有可用的Portal ID，可以在Portal管理中手動配置。",
                            errorDetails = "INSUFFICIENT_PORTAL_PERMISSIONS"
                        )
                    }
                }
                
                response
            }
            else -> {
                // 測試和開發模式：使用原有邏輯
                Log.d(TAG, "Using legacy API for mode: $currentMode")
                val id = portalConfig?.id ?: ApiConfig.DEFAULT_PORTAL_ID
                sendChatMessageLegacy(username, password, baseUrl, message, id)
            }
        }
    }
    
    /**
     * 遞歸嘗試所有備選 Portal ID，直到找到可用的或全部失敗
     */
    private suspend fun tryAlternativePortalIds(
        userPrompt: String,
        uploadedFile: String?,
        alternativeIds: List<String>,
        portalConfig: PortalConfig?,
        sessionCookie: String?
    ): ChatResponse {
        if (alternativeIds.isEmpty()) {
            Log.w(TAG, "No more alternative Portal IDs to try")
            return ChatResponse(
                success = false,
                message = "🚫 所有Portal ID訪問被拒\n\n" +
                         "已嘗試所有可用的Portal ID，但都沒有提交權限。\n\n" +
                         "這通常表示：\n" +
                         "• 用戶賬號缺少Portal提交權限\n" +
                         "• 需要管理員授予適當的Portal訪問權限\n" +
                         "• 或者需要使用不同的Portal配置\n\n" +
                         "💡 建議聯繫系統管理員檢查權限設定。",
                errorDetails = "ALL_PORTALS_ACCESS_DENIED"
            )
        }
        
        val currentPortalId = alternativeIds.first()
        val remainingIds = alternativeIds.drop(1)
        
        Log.d(TAG, "Trying alternative Portal ID: $currentPortalId (remaining: ${remainingIds.size})")
        
        val response = portalApiService.sendPortalChatMessage(
            userPrompt = userPrompt,
            uploadedFile = uploadedFile,
            portalId = currentPortalId,
            alternativePortalIds = emptyList(), // 避免PortalApiService中的重複重試
            portalConfig = portalConfig,
            sessionCookie = sessionCookie
        )
        
        return if (response.success) {
            Log.i(TAG, "Alternative Portal ID $currentPortalId worked successfully")
            response
        } else if (response.errorDetails == "PORTAL_ACCESS_DENIED") {
            Log.w(TAG, "Alternative Portal ID $currentPortalId also denied access, trying next")
            // 遞歸嘗試下一個 Portal ID
            tryAlternativePortalIds(
                userPrompt = userPrompt,
                uploadedFile = uploadedFile,
                alternativeIds = remainingIds,
                portalConfig = portalConfig,
                sessionCookie = sessionCookie
            )
        } else {
            Log.w(TAG, "Alternative Portal ID $currentPortalId failed with different error: ${response.message}")
            response // 返回非權限相關的錯誤，不再嘗試其他ID
        }
    }
    
    private suspend fun sendChatMessageLegacy(
        username: String,
        password: String,
        baseUrl: String,
        message: String,
        id: String = ApiConfig.DEFAULT_PORTAL_ID
    ): ChatResponse = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Sending chat message via backend API")
            
            // 根據後端 API 說明構建請求 JSON
            val requestData = mapOf(
                "message" to message,
                "username" to username,
                "password" to password,
                "baseUrl" to baseUrl,
                "id" to id
            )
            
            val requestJson = gson.toJson(requestData)
            Log.d(TAG, "Request JSON: $requestJson")
            
            val requestBody = requestJson.toRequestBody("application/json".toMediaType())
            
            // 使用配置的後端 API URL
            val chatUrl = ApiConfig.getChatApiUrl()
            
            val request = Request.Builder()
                .url(chatUrl)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "Android AIBridge App")
                .build()

            Log.d(TAG, "Making request to backend: $chatUrl")
            
            val response = httpClient.newCall(request).execute()
            
            Log.d(TAG, "Response code: ${response.code}")
            val responseBody = response.body?.string() ?: ""
            Log.d(TAG, "Response body: $responseBody")
            
            if (response.isSuccessful) {
                Log.i(TAG, "Chat request successful")
                // 根據配置模式處理回應
                if (ApiConfig.isTestMode()) {
                    // 測試模式：返回友好的測試回應
                    val testResponse = when {
                        message.lowercase().contains("hello") || message.lowercase().contains("hi") -> 
                            "👋 您好！我是 AI Bridge 測試助手。\n\n✅ 網路連線測試成功！\n📱 APP 功能正常運作。\n\n${ApiConfig.getCurrentModeDisplayName()}\n${ApiConfig.getModeDescription()}"
                        message.lowercase().contains("test") || message.lowercase().contains("測試") -> 
                            "🧪 測試模式運行中...\n\n✅ 所有系統檢查通過\n🔧 準備就緒，等待真實 AI 服務配置\n\n當前模式：${ApiConfig.getCurrentModeDisplayName()}"
                        message.lowercase().contains("help") || message.lowercase().contains("幫助") -> 
                            "❓ 如何啟用真實 AI 功能：\n\n1️⃣ 修改 ApiConfig.kt 中的 CURRENT_MODE 為 \"PRODUCTION\"\n2️⃣ 確保後端服務正常運行\n3️⃣ 重新建置並安裝 APP"
                        message.lowercase().contains("mode") || message.lowercase().contains("模式") -> 
                            "⚙️ 當前運行模式：${ApiConfig.getCurrentModeDisplayName()}\n\n📋 說明：${ApiConfig.getModeDescription()}\n\n🔄 要切換模式，請修改 ApiConfig.kt 檔案"
                        else -> 
                            "🤖 收到您的訊息：「${message}」\n\n📡 這是測試回應，證明 APP 通訊功能正常。\n🚀 準備好接入真實 AI 服務了！\n\n模式：${ApiConfig.getCurrentModeDisplayName()}"
                    }
                    
                    ChatResponse(
                        success = true,
                        message = testResponse,
                        responseData = null
                    )
                } else {
                    // 生產/開發模式：處理真實後端回應
                    try {
                        val backendResponse = gson.fromJson(responseBody, BackendChatResponse::class.java)
                        if (backendResponse.reply != null) {
                            ChatResponse(
                                success = true,
                                message = backendResponse.reply,
                                responseData = null
                            )
                        } else if (backendResponse.error != null) {
                            ChatResponse(
                                success = false,
                                message = backendResponse.error
                            )
                        } else {
                            Log.w(TAG, "Backend response missing both reply and error fields")
                            ChatResponse(
                                success = false,
                                message = "後端回應格式不正確"
                            )
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse backend response, using plain text: ${e.message}")
                        // 如果 JSON 解析失敗，嘗試將整個回應作為 reply
                        ChatResponse(
                            success = true,
                            message = responseBody.ifEmpty { "AI 回應成功但內容為空" }
                        )
                    }
                }
            } else {
                Log.w(TAG, "Chat request failed with code: ${response.code}")
                val errorMessage = when (response.code) {
                    400 -> "請求格式錯誤"
                    401 -> "認證失敗，請重新登入"
                    403 -> "沒有權限訪問 AI 服務"
                    429 -> "請求過於頻繁，請稍後重試"
                    500 -> "伺服器內部錯誤"
                    502, 503, 504 -> "伺服器暫時無法使用"
                    else -> "請求失敗 (HTTP ${response.code})"
                }
                
                ChatResponse(
                    success = false,
                    message = errorMessage,
                    errorDetails = responseBody
                )
            }
            
        } catch (e: UnknownHostException) {
            Log.e(TAG, "DNS resolution failed: ${e.message}")
            val errorMessage = networkUtils.getNetworkErrorMessage(e.message ?: "DNS resolution failed")
            ChatResponse(success = false, message = errorMessage)
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Connection timeout: ${e.message}")
            ChatResponse(success = false, message = "連線逾時：請檢查網路環境或稍後重試")
        } catch (e: ConnectException) {
            Log.e(TAG, "Connection failed: ${e.message}")
            ChatResponse(success = false, message = "連線失敗：無法連接到 AI 服務")
        } catch (e: IOException) {
            Log.e(TAG, "Network I/O error: ${e.message}")
            val errorMessage = networkUtils.getNetworkErrorMessage(e.message ?: "Network error")
            ChatResponse(success = false, message = errorMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Chat request exception: ${e.message}", e)
            val errorMessage = networkUtils.getNetworkErrorMessage(e.message ?: "Unknown error")
            ChatResponse(success = false, message = errorMessage)
        }
    }
}

data class ChatResponse(
    val success: Boolean,
    val message: String,
    val responseData: BackendChatResponse? = null,
    val errorDetails: String? = null
)
