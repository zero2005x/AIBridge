package com.aibridge.chat.data.api

import android.util.Log
import com.aibridge.chat.config.ApiConfig
import com.aibridge.chat.domain.model.PortalConfig
import com.aibridge.chat.utils.NetworkUtils
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import java.net.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Portal API 服務
 * 負責與Portal API進行通信，處理聊天請求
 */
@Singleton
class PortalApiService @Inject constructor(
    private val httpClient: OkHttpClient,
    private val gson: Gson,
    private val networkUtils: NetworkUtils
) {
    companion object {
        private const val TAG = "PortalApiService"
    }

    /**
     * 向Portal API發送聊天訊息
     * 
     * @param userPrompt 用戶提示文字
     * @param uploadedFile 上傳的文件內容（可選）
     * @param portalId Portal ID
     * @param portalConfig Portal配置（可選）
     * @param sessionCookie 認證Cookie
     * @param alternativePortalIds 備用Portal ID列表
     * @return ChatResponse 聊天回應
     */
    suspend fun sendPortalChatMessage(
        userPrompt: String,
        uploadedFile: String?,
        portalId: String,
        portalConfig: PortalConfig? = null,
        sessionCookie: String? = null,
        alternativePortalIds: List<String> = emptyList()
    ): ChatResponse = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Sending chat message to Portal API")
            Log.d(TAG, "Using existing authentication session for chat request")
            
            // 調用主要的聊天請求方法
            val response = sendChatRequest(userPrompt, uploadedFile, portalId, portalConfig, sessionCookie)
            
            // 如果收到403錯誤且有替代的Portal ID，嘗試使用它們
            if (!response.success && response.message?.contains("403") == true && alternativePortalIds.isNotEmpty()) {
                Log.w(TAG, "Portal ID $portalId failed with 403, trying alternatives: $alternativePortalIds")
                
                for (alternativeId in alternativePortalIds) {
                    if (alternativeId != portalId) {
                        Log.d(TAG, "Trying alternative Portal ID: $alternativeId")
                        val alternativeResponse = sendChatRequest(userPrompt, uploadedFile, alternativeId, portalConfig, sessionCookie)
                        
                        if (alternativeResponse.success) {
                            Log.i(TAG, "Successfully used alternative Portal ID: $alternativeId")
                            return@withContext alternativeResponse
                        } else {
                            Log.w(TAG, "Alternative Portal ID $alternativeId also failed: ${alternativeResponse.message}")
                        }
                    }
                }
                
                Log.e(TAG, "All Portal IDs failed, returning original error")
            }
            
            return@withContext response
            
        } catch (e: UnknownHostException) {
            Log.e(TAG, "DNS resolution failed: ${e.message}")
            val errorMessage = networkUtils.getNetworkErrorMessage(e.message ?: "DNS resolution failed")
            ChatResponse(success = false, message = errorMessage)
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Connection timeout: ${e.message}")
            ChatResponse(success = false, message = "連線逾時：請檢查網路環境或稍後重試")
        } catch (e: ConnectException) {
            Log.e(TAG, "Connection failed: ${e.message}")
            ChatResponse(success = false, message = "連線失敗：無法連接到Portal服務")
        } catch (e: IOException) {
            Log.e(TAG, "Network I/O error: ${e.message}")
            val errorMessage = networkUtils.getNetworkErrorMessage(e.message ?: "Network error")
            ChatResponse(success = false, message = errorMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Portal chat request exception: ${e.message}", e)
            val errorMessage = networkUtils.getNetworkErrorMessage(e.message ?: "Unknown error")
            ChatResponse(success = false, message = errorMessage)
        }
    }
    
    /**
     * 發送聊天請求到Portal API
     */
    private suspend fun sendChatRequest(
        userPrompt: String,
        uploadedFile: String?,
        portalId: String,
        portalConfig: PortalConfig? = null,
        sessionCookie: String? = null
    ): ChatResponse = withContext(Dispatchers.IO) {
        try {
            val chatUrl = "${ApiConfig.BACKEND_BASE_URL}${ApiConfig.CHAT_ENDPOINT}?id=$portalId&action=completion"
            
            Log.d(TAG, "Portal API URL: $chatUrl")
            Log.d(TAG, "Portal ID: $portalId")
            Log.d(TAG, "User Prompt: $userPrompt")
            
            // 直接構建POST請求到completion端點
            val formBodyBuilder = FormBody.Builder()
            
            // 如果有PortalConfig，使用其中的自定義參數
            if (portalConfig != null && portalConfig.parameters.isNotEmpty()) {
                Log.d(TAG, "Using custom parameters from PortalConfig: ${portalConfig.parameters}")
                // 添加所有自定義參數
                for ((key, parameter) in portalConfig.parameters) {
                    Log.d(TAG, "Adding custom parameter: $key = ${parameter.value}")
                    formBodyBuilder.add(key, parameter.value)
                }
            } else {
                // 使用默認參數
                Log.d(TAG, "Using default parameters")
                
                // 添加用戶提示
                formBodyBuilder.add("USERPROMPT", userPrompt)
                
                // 添加上傳文件（如果有）
                if (!uploadedFile.isNullOrEmpty()) {
                    // 如果有文件內容，需要編碼為base64格式
                    val base64Content = android.util.Base64.encodeToString(
                        uploadedFile.toByteArray(Charsets.UTF_8), 
                        android.util.Base64.NO_WRAP
                    )
                    formBodyBuilder.add("USERUPLOADFILE", "data:text/plain;base64,$base64Content")
                } else {
                    // 添加空的文件字段
                    formBodyBuilder.add("USERUPLOADFILE", "data:application/octet-stream;base64,")
                }
            }
            
            val formBody = formBodyBuilder.build()
            
            // 構建POST請求
            val requestBuilder = Request.Builder()
                .url(chatUrl)
                .post(formBody)
                .addHeader("accept", "application/json, text/javascript, */*; q=0.01")
                .addHeader("accept-language", "zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7")
                .addHeader("content-type", "application/x-www-form-urlencoded; charset=UTF-8")
                .addHeader("sec-ch-ua", "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Google Chrome\";v=\"138\"")
                .addHeader("sec-ch-ua-mobile", "?0")
                .addHeader("sec-ch-ua-platform", "\"Windows\"")
                .addHeader("sec-fetch-dest", "empty")
                .addHeader("sec-fetch-mode", "cors")
                .addHeader("sec-fetch-site", "same-origin")
                .addHeader("x-kl-kis-ajax-request", "Ajax_Request")
                .addHeader("x-requested-with", "XMLHttpRequest")
                .addHeader("Referer", "${ApiConfig.BACKEND_BASE_URL}/")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            
            // 添加 Session Cookie 如果可用
            if (!sessionCookie.isNullOrEmpty()) {
                Log.d(TAG, "Adding session cookie to POST request")
                requestBuilder.addHeader("Cookie", sessionCookie)
            }

            val request = requestBuilder.build()

            Log.d(TAG, "Making POST request to Portal API: $chatUrl")
            
            val response = httpClient.newCall(request).execute()
            
            Log.d(TAG, "Portal API Response code: ${response.code}")
            val responseBody = response.body?.string() ?: ""
            Log.d(TAG, "Portal API Response body: $responseBody")
            
            return@withContext if (response.isSuccessful) {
                Log.i(TAG, "Portal chat request successful")
                
                // 檢測是否收到登入頁面（會話過期）
                if (responseBody.contains("智能客服 - 登入") || responseBody.contains("<title>智能客服 - 登入</title>") || 
                    responseBody.contains("action=\"/wise/wiseadm/s/login\"")) {
                    Log.w(TAG, "Received login page - session expired")
                    return@withContext ChatResponse(
                        success = false,
                        message = "會話已過期，需要重新登入",
                        errorDetails = "LOGIN_REQUIRED"
                    )
                }
                
                try {
                    // 解析Portal API的回應格式
                    val portalResponse = gson.fromJson(responseBody, PortalApiResponse::class.java)
                    
                    if (portalResponse != null && !portalResponse.completion.isNullOrEmpty()) {
                        ChatResponse(
                            success = true,
                            message = portalResponse.completion,
                            responseData = null
                        )
                    } else {
                        Log.w(TAG, "Portal response missing completion field")
                        ChatResponse(
                            success = false,
                            message = "Portal回應格式不正確"
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse Portal response: ${e.message}")
                    
                    // 檢查是否是HTML回應（可能是登入頁面或其他錯誤頁面）
                    if (responseBody.contains("<!DOCTYPE html>") || responseBody.contains("<html")) {
                        Log.w(TAG, "Received HTML response instead of JSON - likely session expired or error page")
                        return@withContext ChatResponse(
                            success = false,
                            message = "收到HTML回應而非JSON數據，會話可能已過期",
                            errorDetails = "LOGIN_REQUIRED"
                        )
                    }
                    
                    // 如果不是HTML，嘗試將整個回應作為消息
                    ChatResponse(
                        success = true,
                        message = responseBody.ifEmpty { "Portal回應成功但內容為空" }
                    )
                }
            } else {
                Log.w(TAG, "Portal chat request failed with code: ${response.code}")
                
                // 檢測 403 錯誤頁面中的具體原因
                if (response.code == 403) {
                    // 檢查是否為 Portal ID 權限問題
                    val is403ForbiddenPage = responseBody.contains("403 - Forbidden") && 
                                           responseBody.contains("Your request to the page are not authorized")
                    
                    if (is403ForbiddenPage) {
                        Log.w(TAG, "Detected 403 Forbidden - Portal ID access denied")
                        return@withContext ChatResponse(
                            success = false,
                            message = "🔒 Portal提交權限不足 (Portal ID: $portalId)\n\n" +
                                     "當前用戶可以查看此Portal，但沒有提交權限。\n\n" +
                                     "可能的原因：\n" +
                                     "• 用戶權限級別不足\n" +
                                     "• Portal需要特殊授權\n" +
                                     "• 管理員尚未開放此Portal的提交功能\n\n" +
                                     "💡 請聯繫系統管理員獲取適當權限，或嘗試使用其他Portal配置。",
                            errorDetails = "PORTAL_ACCESS_DENIED"
                        )
                    }
                }
                
                val errorMessage = when (response.code) {
                    302 -> "需要重新認證或登入Portal服務"
                    400 -> "請求格式錯誤"
                    401 -> "認證失敗，請檢查登入狀態"
                    403 -> "沒有權限訪問Portal服務 - 請聯繫管理員獲取權限"
                    404 -> "Portal服務端點不存在"
                    429 -> "請求過於頻繁，請稍後重試"
                    500 -> "Portal伺服器內部錯誤"
                    502, 503, 504 -> "Portal伺服器暫時無法使用"
                    else -> "Portal請求失敗 (HTTP ${response.code})"
                }
                
                ChatResponse(
                    success = false,
                    message = errorMessage,
                    errorDetails = responseBody
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Chat request failed: ${e.message}", e)
            val errorMessage = networkUtils.getNetworkErrorMessage(e.message ?: "Unknown error")
            ChatResponse(success = false, message = errorMessage)
        }
    }
}

/**
 * Portal API 回應格式
 */
data class PortalApiResponse(
    val completion: String?,
    val usage: PortalUsage?
)

data class PortalUsage(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0
)