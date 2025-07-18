package com.aibridge.chat.data.api

import android.util.Log
import com.aibridge.chat.config.ApiConfig
import com.aibridge.chat.domain.model.LoginResult
import com.aibridge.chat.domain.model.PortalCredentials
import com.aibridge.chat.utils.NetworkUtils
import com.aibridge.chat.utils.PortalEndpointDiscovery
import com.aibridge.chat.utils.PortalListDiscovery
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
class AuthApiService @Inject constructor(
    private val httpClient: OkHttpClient,
    private val networkUtils: NetworkUtils,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "AuthApiService"
        private const val FALLBACK_PORTAL_LOGIN_PATH = "/wise/wiseadm/s/subadmin/2595af81-c151-47eb-9f15-d17e0adbe3b4/login"
        private const val PORTAL_ACCESS_PATH = "/wise/wiseadm/s/promptportal/portal"
    }

    private val endpointDiscovery = PortalEndpointDiscovery(httpClient)
    private val portalListDiscovery = PortalListDiscovery(httpClient, gson)

    suspend fun checkLogin(
        username: String,
        password: String,
        baseUrl: String
    ): LoginResult {
        Log.d(TAG, "Starting Portal authentication for user: $username")
        
        val credentials = PortalCredentials(
            username = username,
            password = password, 
            baseUrl = baseUrl
        )
        
        return performLogin(credentials)
    }

    suspend fun performFreshLogin(
        username: String,
        password: String,
        baseUrl: String
    ): LoginResult {
        Log.d(TAG, "Performing fresh login for user: $username")
        
        val credentials = PortalCredentials(
            username = username,
            password = password,
            baseUrl = baseUrl
        )
        
        return performLogin(credentials)
    }

    private suspend fun performLogin(credentials: PortalCredentials): LoginResult {
        return withContext(Dispatchers.IO) {
            try {
                // Pre-flight network checks
                Log.d(TAG, "Performing pre-flight network checks")
                if (!networkUtils.isNetworkAvailable()) {
                    Log.w(TAG, "No network connectivity available")
                    return@withContext LoginResult(
                        success = false, 
                        message = "網路連線失敗：請檢查您的網路連線狀態"
                    )
                }

                // Check if we can resolve the target hostname
                val baseUrlHost = credentials.baseUrl.replace("https://", "").replace("http://", "").split("/")[0]
                if (!networkUtils.canResolveHost(baseUrlHost)) {
                    Log.w(TAG, "Cannot resolve hostname: $baseUrlHost")
                    return@withContext LoginResult(
                        success = false, 
                        message = "無法連接到伺服器：請檢查伺服器位址是否正確"
                    )
                }

                Log.d(TAG, "Network checks passed, proceeding with login")
                
                // Step 0: 動態發現端點（如果尚未發現）
                Log.d(TAG, "Step 0: Discovering Portal endpoints")
                val endpoints = endpointDiscovery.discoverPortalEndpoints(credentials.baseUrl)
                if (endpoints.discoverySuccess && endpoints.loginPath != null) {
                    Log.d(TAG, "Dynamic endpoints discovered successfully")
                    Log.d(TAG, "Login Path: ${endpoints.loginPath}")
                    Log.d(TAG, "Portal Path: ${endpoints.portalPath}")
                    Log.d(TAG, "Chat Endpoint: ${endpoints.chatEndpoint}")
                    Log.d(TAG, "Detected UUID: ${endpoints.detectedUuid}")
                    
                    // 更新 ApiConfig 的動態端點
                    ApiConfig.setDynamicEndpoints(
                        portalId = endpoints.detectedUuid,
                        loginUrl = if (endpoints.loginPath != null) "${credentials.baseUrl}${endpoints.loginPath}" else null,
                        completionUrl = if (endpoints.chatEndpoint != null) "${credentials.baseUrl}${endpoints.chatEndpoint}" else null
                    )
                } else {
                    Log.w(TAG, "Failed to discover dynamic endpoints, using fallback")
                }
                
                Log.d(TAG, "Step 1: Getting login page and initial cookies")
                
                // Step 1: 取得登入頁面和初始 cookies（使用動態發現的或回退的URL）
                val loginPageUrl = ApiConfig.getLoginUrl() ?: "${credentials.baseUrl}$FALLBACK_PORTAL_LOGIN_PATH"
                Log.d(TAG, "Login page URL: $loginPageUrl")
                
                val loginPageResponse = httpClient.newCall(
                    Request.Builder()
                        .url(loginPageUrl)
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36")
                        .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                        .addHeader("Accept-Language", "zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7")
                        .get()
                        .build()
                ).execute()

                Log.d(TAG, "Login page response: ${loginPageResponse.code}")
                
                if (!loginPageResponse.isSuccessful) {
                    Log.e(TAG, "Failed to get login page: ${loginPageResponse.code}")
                    return@withContext LoginResult(success = false, message = "無法取得登入頁面: ${loginPageResponse.code}")
                }

                val initialCookies = extractCookies(loginPageResponse)
                Log.d(TAG, "Initial cookies: $initialCookies")

                // Step 2: 提交登入表單
                Log.d(TAG, "Step 2: Submitting login form")
                
                val formBody = FormBody.Builder()
                    .add("loginName", credentials.username)
                    .add("intumitPswd", credentials.password)
                    .add("selectedLocale", "zh_TW")
                    .add("keepUser", "false")
                    .build()

                val loginResponse = httpClient.newCall(
                    Request.Builder()
                        .url(loginPageUrl)
                        .post(formBody)
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .addHeader("Cookie", initialCookies)
                        .addHeader("Origin", credentials.baseUrl)
                        .addHeader("Referer", loginPageUrl)
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36")
                        .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                        .addHeader("Accept-Language", "zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7")
                        .addHeader("Upgrade-Insecure-Requests", "1")
                        .build()
                ).execute()

                Log.d(TAG, "Login response: ${loginResponse.code}")

                return@withContext when (loginResponse.code) {
                    302 -> {
                        val sessionCookies = extractSessionCookies(loginResponse)
                        val location = loginResponse.header("location")
                        
                        Log.d(TAG, "Redirect location: $location")
                        Log.d(TAG, "Session cookies: $sessionCookies")

                        if (location?.contains("login") == true) {
                            Log.w(TAG, "Login failed - redirected back to login page")
                            LoginResult(success = false, message = "登入失敗：用戶名或密碼錯誤")
                        } else {
                            Log.i(TAG, "Login successful")
                            LoginResult(success = true, message = "登入成功", sessionCookie = sessionCookies)
                        }
                    }
                    200 -> {
                        // 檢查是否有錯誤訊息
                        val responseBody = loginResponse.body?.string() ?: ""
                        Log.d(TAG, "Response body length: ${responseBody.length}")
                        Log.d(TAG, "Response body preview (first 500 chars): ${responseBody.take(500)}")
                        
                        // 更精確的錯誤檢測 - 只檢查明確的錯誤訊息
                        val hasError = responseBody.contains("登入失敗") ||
                                     responseBody.contains("帳號或密碼錯誤") ||
                                     responseBody.contains("用戶名或密碼錯誤") ||
                                     responseBody.contains("Invalid username or password") ||
                                     responseBody.contains("Login failed") ||
                                     responseBody.contains("authentication failed", ignoreCase = true) ||
                                     responseBody.contains("帳號不存在") ||
                                     responseBody.contains("密碼錯誤") ||
                                     responseBody.contains("登入錯誤") ||
                                     // 檢查是否是登入表單頁面（包含輸入欄位）
                                     (responseBody.contains("loginName") && responseBody.contains("intumitPswd") && responseBody.contains("type=\"password\""))
                        
                        Log.d(TAG, "Error indicators found: $hasError")
                        
                        if (hasError) {
                            Log.w(TAG, "Login failed - found error in response")
                            LoginResult(success = false, message = "登入失敗：用戶名或密碼錯誤")
                        } else {
                            val sessionCookies = extractSessionCookies(loginResponse)
                            Log.i(TAG, "Login successful with 200 response")
                            LoginResult(success = true, message = "登入成功", sessionCookie = sessionCookies)
                        }
                    }
                    else -> {
                        Log.e(TAG, "Unexpected response code: ${loginResponse.code}")
                        LoginResult(success = false, message = "登入失敗：HTTP ${loginResponse.code}")
                    }
                }
            } catch (e: UnknownHostException) {
                Log.e(TAG, "DNS resolution failed: ${e.message}")
                val errorMessage = networkUtils.getNetworkErrorMessage(e.message ?: "DNS resolution failed")
                LoginResult(success = false, message = errorMessage)
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Connection timeout: ${e.message}")
                LoginResult(success = false, message = "連線逾時：請檢查網路環境或稍後重試")
            } catch (e: ConnectException) {
                Log.e(TAG, "Connection failed: ${e.message}")
                LoginResult(success = false, message = "連線失敗：無法連接到伺服器")
            } catch (e: IOException) {
                Log.e(TAG, "Network I/O error: ${e.message}")
                val errorMessage = networkUtils.getNetworkErrorMessage(e.message ?: "Network error")
                LoginResult(success = false, message = errorMessage)
            } catch (e: Exception) {
                Log.e(TAG, "Login exception: ${e.message}", e)
                val errorMessage = networkUtils.getNetworkErrorMessage(e.message ?: "Unknown error")
                LoginResult(success = false, message = errorMessage)
            }
        }
    }

    suspend fun checkAccess(sessionCookie: String, baseUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Checking portal access using simplified method")
                
                // 使用新的Portal訪問測試方法
                val hasAccess = portalListDiscovery.testPortalAccess(baseUrl, sessionCookie)
                
                Log.d(TAG, "Portal access result: $hasAccess")
                return@withContext hasAccess
                
            } catch (e: Exception) {
                Log.e(TAG, "Access check exception: ${e.message}", e)
                false
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun logout(sessionCookie: String): Boolean {
        return try {
            // Portal logout is typically handled by clearing cookies on client side
            // The sessionCookie parameter is kept for API consistency
            Log.d(TAG, "Logout requested")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Logout exception: ${e.message}", e)
            false
        }
    }

    private fun extractCookies(response: okhttp3.Response): String {
        val cookies = response.headers("Set-Cookie")
        Log.d(TAG, "Extracting cookies from ${cookies.size} Set-Cookie headers")
        
        // 只取第一個 cookie 的主要部分，與後端邏輯一致
        return if (cookies.isNotEmpty()) {
            val firstCookie = cookies[0].split(",")[0].split(";")[0]
            Log.d(TAG, "Extracted initial cookie: $firstCookie")
            firstCookie
        } else {
            Log.w(TAG, "No cookies found in response")
            ""
        }
    }

    private fun extractSessionCookies(response: okhttp3.Response): String {
        val cookies = response.headers("Set-Cookie")
        Log.d(TAG, "Extracting session cookies from ${cookies.size} Set-Cookie headers")
        
        val sessionCookies = cookies.joinToString("; ") { cookie ->
            cookie.substringBefore(";")
        }
        
        Log.d(TAG, "Extracted session cookies: $sessionCookies")
        return sessionCookies
    }
    
    /**
     * 發現用戶可訪問的Portal ID（使用Portal列表API）
     */
    suspend fun discoverAvailablePortalIds(sessionCookie: String, baseUrl: String): List<String> {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "Discovering available Portal IDs using Portal list API")
            Log.d(TAG, "Base URL: $baseUrl")
            Log.d(TAG, "Session Cookie: ${sessionCookie.take(50)}...")
            
            try {
                // 使用Portal列表發現服務
                val discoveredPortals = portalListDiscovery.discoverPortals(baseUrl, sessionCookie)
                
                if (discoveredPortals.success && discoveredPortals.portals.isNotEmpty()) {
                    val availablePortalIds = discoveredPortals.portals.map { it.id.toString() }
                    
                    Log.i(TAG, "Discovery complete via Portal list API. Available Portal IDs: $availablePortalIds")
                    discoveredPortals.portals.forEach { portal ->
                        Log.d(TAG, "Portal ${portal.id}: ${portal.name} - ${portal.description}")
                    }
                    
                    return@withContext availablePortalIds
                } else {
                    Log.w(TAG, "Portal list discovery failed: ${discoveredPortals.message}")
                    
                    // 如果Portal列表API失敗，回退到簡單的存取檢查
                    Log.d(TAG, "Falling back to access check only")
                    return@withContext emptyList()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during Portal discovery: ${e.message}", e)
                return@withContext emptyList()
            }
        }
    }
}
