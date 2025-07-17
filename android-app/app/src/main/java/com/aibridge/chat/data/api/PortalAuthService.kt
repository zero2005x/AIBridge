package com.aibridge.chat.data.api

import android.util.Log
import com.aibridge.chat.config.ApiConfig
import com.aibridge.chat.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import java.net.UnknownHostException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.ConnectException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortalAuthService @Inject constructor(
    private val httpClient: OkHttpClient,
    private val networkUtils: NetworkUtils,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "PortalAuthService"
        private const val LOGIN_PATH = "/wise/wiseadm/s/subadmin/2595af81-c151-47eb-9f15-d17e0adbe3b4/login"
    }

    private var sessionCookie: String? = null

    suspend fun login(username: String, password: String): PortalAuthResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting Portal login process")
            
            // 構建登入URL
            val loginUrl = "${ApiConfig.BACKEND_BASE_URL}$LOGIN_PATH"
            Log.d(TAG, "Login URL: $loginUrl")
            
            // 構建登入請求體
            val formBody = FormBody.Builder()
                .add("loginName", username)
                .add("intumitPswd", password)
                .add("selectedLocale", "zh_TW")
                .build()
            
            val request = Request.Builder()
                .url(loginUrl)
                .post(formBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .addHeader("Accept-Language", "zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            Log.d(TAG, "Making login request to Portal")
            
            val response = httpClient.newCall(request).execute()
            
            Log.d(TAG, "Login response code: ${response.code}")
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                // 檢查回應是否包含登入成功的指標
                if (responseBody.contains("登入成功") || responseBody.contains("success") || 
                    response.headers("Set-Cookie").isNotEmpty()) {
                    
                    // 提取Session Cookie
                    val cookies = response.headers("Set-Cookie")
                    if (cookies.isNotEmpty()) {
                        sessionCookie = cookies.joinToString("; ")
                        Log.d(TAG, "Session cookie extracted: ${sessionCookie?.take(50)}...")
                    }
                    
                    Log.i(TAG, "Portal login successful")
                    PortalAuthResult(
                        success = true,
                        message = "登入成功",
                        sessionCookie = sessionCookie
                    )
                } else {
                    Log.w(TAG, "Login appears unsuccessful based on response content")
                    PortalAuthResult(
                        success = false,
                        message = "登入失敗：用戶名或密碼錯誤"
                    )
                }
            } else {
                Log.w(TAG, "Login request failed with code: ${response.code}")
                val errorMessage = when (response.code) {
                    400 -> "登入請求格式錯誤"
                    401 -> "認證失敗：用戶名或密碼錯誤"
                    403 -> "帳戶被鎖定或沒有權限"
                    404 -> "登入頁面不存在"
                    429 -> "嘗試次數過多，請稍後重試"
                    500 -> "伺服器內部錯誤"
                    502, 503, 504 -> "伺服器暫時無法使用"
                    else -> "登入失敗 (HTTP ${response.code})"
                }
                
                PortalAuthResult(
                    success = false,
                    message = errorMessage
                )
            }
            
        } catch (e: UnknownHostException) {
            Log.e(TAG, "DNS resolution failed during login: ${e.message}")
            PortalAuthResult(success = false, message = "網路連線失敗：無法連接到Portal服務")
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Connection timeout during login: ${e.message}")
            PortalAuthResult(success = false, message = "連線逾時：請檢查網路環境")
        } catch (e: ConnectException) {
            Log.e(TAG, "Connection failed during login: ${e.message}")
            PortalAuthResult(success = false, message = "連線失敗：無法連接到Portal服務")
        } catch (e: IOException) {
            Log.e(TAG, "Network I/O error during login: ${e.message}")
            PortalAuthResult(success = false, message = "網路錯誤：${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Portal login exception: ${e.message}", e)
            PortalAuthResult(success = false, message = "登入過程發生錯誤：${e.message}")
        }
    }

    /**
     * 獲取當前的Session Cookie
     */
    fun getSessionCookie(): String? = sessionCookie

    /**
     * 清除Session
     */
    fun clearSession() {
        sessionCookie = null
        Log.d(TAG, "Portal session cleared")
    }

    /**
     * 檢查是否已登入
     */
    fun isLoggedIn(): Boolean = !sessionCookie.isNullOrEmpty()
}

/**
 * Portal 認證結果
 */
data class PortalAuthResult(
    val success: Boolean,
    val message: String,
    val sessionCookie: String? = null
)
