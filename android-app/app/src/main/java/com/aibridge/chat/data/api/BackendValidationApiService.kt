package com.aibridge.chat.data.api

import android.util.Log
import com.aibridge.chat.config.ApiConfig
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

/**
 * 後端驗證 API 服務
 * 提供登入檢查和存取權限檢查功能
 */
@Singleton
class BackendValidationApiService @Inject constructor(
    private val httpClient: OkHttpClient,
    private val networkUtils: NetworkUtils,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "BackendValidationApiService"
    }

    /**
     * 檢查登入憑證
     * 對應後端的 /api/check-login 端點
     */
    suspend fun checkLogin(
        username: String,
        password: String,
        baseUrl: String
    ): ValidationResponse = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking login credentials")
            
            val requestData = mapOf(
                "username" to username,
                "password" to password,
                "baseUrl" to baseUrl
            )
            
            val requestJson = gson.toJson(requestData)
            val requestBody = requestJson.toRequestBody("application/json".toMediaType())
            
            val checkUrl = ApiConfig.getCheckLoginApiUrl()
            
            val request = Request.Builder()
                .url(checkUrl)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build()

            Log.d(TAG, "Making login check request to: $checkUrl")
            
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            Log.d(TAG, "Login check response code: ${response.code}")
            Log.d(TAG, "Login check response: $responseBody")
            
            if (response.isSuccessful) {
                try {
                    // 預期回應格式：{ "isLoggedIn": true/false }
                    val loginResponse = gson.fromJson(responseBody, LoginCheckResponse::class.java)
                    ValidationResponse(
                        success = true,
                        isValid = loginResponse.isLoggedIn,
                        message = if (loginResponse.isLoggedIn) "登入憑證有效" else "登入憑證無效"
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse login check response: ${e.message}")
                    ValidationResponse(
                        success = false,
                        isValid = false,
                        message = "登入檢查回應格式錯誤"
                    )
                }
            } else {
                ValidationResponse(
                    success = false,
                    isValid = false,
                    message = "登入檢查失敗 (HTTP ${response.code})"
                )
            }
            
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Network error during login check: ${e.message}")
            ValidationResponse(
                success = false,
                isValid = false,
                message = "網路連線錯誤，無法檢查登入狀態"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error during login check: ${e.message}", e)
            ValidationResponse(
                success = false,
                isValid = false,
                message = "登入檢查失敗: ${e.message}"
            )
        }
    }

    /**
     * 檢查 Portal 存取權限
     * 對應後端的 /api/check-access 端點
     */
    suspend fun checkAccess(
        username: String,
        password: String,
        baseUrl: String
    ): ValidationResponse = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking Portal access permissions")
            
            val requestData = mapOf(
                "username" to username,
                "password" to password,
                "baseUrl" to baseUrl
            )
            
            val requestJson = gson.toJson(requestData)
            val requestBody = requestJson.toRequestBody("application/json".toMediaType())
            
            val checkUrl = ApiConfig.getCheckAccessApiUrl()
            
            val request = Request.Builder()
                .url(checkUrl)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build()

            Log.d(TAG, "Making access check request to: $checkUrl")
            
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            Log.d(TAG, "Access check response code: ${response.code}")
            Log.d(TAG, "Access check response: $responseBody")
            
            if (response.isSuccessful) {
                try {
                    // 預期回應格式：{ "hasAccess": true/false }
                    val accessResponse = gson.fromJson(responseBody, AccessCheckResponse::class.java)
                    ValidationResponse(
                        success = true,
                        isValid = accessResponse.hasAccess,
                        message = if (accessResponse.hasAccess) "有 Portal 存取權限" else "無 Portal 存取權限"
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse access check response: ${e.message}")
                    ValidationResponse(
                        success = false,
                        isValid = false,
                        message = "存取權限檢查回應格式錯誤"
                    )
                }
            } else {
                ValidationResponse(
                    success = false,
                    isValid = false,
                    message = "存取權限檢查失敗 (HTTP ${response.code})"
                )
            }
            
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Network error during access check: ${e.message}")
            ValidationResponse(
                success = false,
                isValid = false,
                message = "網路連線錯誤，無法檢查存取權限"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error during access check: ${e.message}", e)
            ValidationResponse(
                success = false,
                isValid = false,
                message = "存取權限檢查失敗: ${e.message}"
            )
        }
    }
}

// 資料模型
data class ValidationResponse(
    val success: Boolean,
    val isValid: Boolean,
    val message: String
)

data class LoginCheckResponse(
    val isLoggedIn: Boolean
)

data class AccessCheckResponse(
    val hasAccess: Boolean
)
