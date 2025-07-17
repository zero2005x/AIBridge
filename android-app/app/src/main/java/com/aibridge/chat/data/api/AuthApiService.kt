package com.aibridge.chat.data.api

import android.util.Log
import com.aibridge.chat.domain.model.LoginResult
import com.aibridge.chat.domain.model.PortalCredentials
import com.aibridge.chat.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.UnknownHostException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.ConnectException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthApiService @Inject constructor(
    private val httpClient: OkHttpClient,
    private val networkUtils: NetworkUtils
) {
    companion object {
        private const val TAG = "AuthApiService"
        private const val PORTAL_LOGIN_PATH = "/wise/wiseadm/s/subadmin/2595af81-c151-47eb-9f15-d17e0adbe3b4/login"
        private const val PORTAL_ACCESS_PATH = "/wise/wiseadm/s/promptportal/portal"
        
        // 常見的Portal ID列表，用於測試用戶權限
        private val COMMON_PORTAL_IDS = listOf("1", "2", "3", "4", "5", "10", "11", "12", "13", "14", "15", "16", "20", "21", "100")
    }

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
                Log.d(TAG, "Step 1: Getting login page and initial cookies")
                
                // Step 1: 取得登入頁面和初始 cookies
                val loginPageUrl = "${credentials.baseUrl}$PORTAL_LOGIN_PATH"
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
                Log.d(TAG, "Checking portal access")
                
                val accessUrl = "$baseUrl$PORTAL_ACCESS_PATH"
                Log.d(TAG, "Portal access URL: $accessUrl")
                
                val response = httpClient.newCall(
                    Request.Builder()
                        .url(accessUrl)
                        .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                        .addHeader("Accept-Language", "zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7")
                        .addHeader("Cookie", sessionCookie)
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36")
                        .addHeader("Upgrade-Insecure-Requests", "1")
                        .build()
                ).execute()

                Log.d(TAG, "Access check response: ${response.code}")
                
                val hasAccess = response.isSuccessful && response.code != 302
                
                if (hasAccess) {
                    // 檢查回應內容是否包含預期的 Portal 內容
                    val responseBody = response.body?.string() ?: ""
                    Log.d(TAG, "Portal access response body preview: ${responseBody.take(200)}")
                    
                    val containsPortalContent = responseBody.contains("portal") || 
                                             responseBody.contains("promptportal") ||
                                             responseBody.contains("Portal") ||
                                             (!responseBody.contains("login") && responseBody.length > 100)
                    
                    Log.d(TAG, "Portal access granted: $containsPortalContent")
                    Log.d(TAG, "Response contains 'portal': ${responseBody.contains("portal")}")
                    Log.d(TAG, "Response contains 'promptportal': ${responseBody.contains("promptportal")}")
                    Log.d(TAG, "Response contains 'Portal': ${responseBody.contains("Portal")}")
                    Log.d(TAG, "Response does not contain 'login': ${!responseBody.contains("login")}")
                    Log.d(TAG, "Response length: ${responseBody.length}")
                    
                    containsPortalContent
                } else {
                    Log.w(TAG, "Portal access denied: HTTP ${response.code}")
                    false
                }
            } catch (e: UnknownHostException) {
                Log.e(TAG, "DNS resolution failed during access check: ${e.message}")
                false
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Timeout during access check: ${e.message}")
                false
            } catch (e: ConnectException) {
                Log.e(TAG, "Connection failed during access check: ${e.message}")
                false
            } catch (e: IOException) {
                Log.e(TAG, "Network I/O error during access check: ${e.message}")
                false
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
     * 發現用戶可訪問的Portal ID
     */
    suspend fun discoverAvailablePortalIds(sessionCookie: String, baseUrl: String): List<String> {
        return withContext(Dispatchers.IO) {
            val availablePortalIds = mutableListOf<String>()
            
            Log.d(TAG, "Discovering available Portal IDs for user")
            Log.d(TAG, "Base URL: $baseUrl")
            Log.d(TAG, "Session Cookie: ${sessionCookie.take(50)}...")
            
            // 先嘗試訪問Portal主頁，看是否有權限信息
            try {
                val portalMainUrl = "$baseUrl/wise/wiseadm/s/promptportal/portal"
                Log.d(TAG, "Accessing Portal main page: $portalMainUrl")
                val mainPageResponse = httpClient.newCall(
                    Request.Builder()
                        .url(portalMainUrl)
                        .addHeader("Cookie", sessionCookie)
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .build()
                ).execute()
                
                Log.d(TAG, "Portal main page response: ${mainPageResponse.code}")
                if (mainPageResponse.isSuccessful) {
                    val mainPageBody = mainPageResponse.body?.string() ?: ""
                    Log.d(TAG, "Portal main page response length: ${mainPageBody.length}")
                    
                    // 更全面的Portal ID模式匹配
                    val patterns = listOf(
                        Regex("""form\?id=(\d+)"""),  // 原有模式
                        Regex("""/portal/form\?id=(\d+)"""),  // 完整路径模式
                        Regex("""portal\.form\?id=(\d+)"""),  // JS模式
                        Regex("""data-portal-id=["'](\d+)["']"""),  // HTML data属性
                        Regex("""portalId["\s]*[:=]["\s]*(\d+)"""),  // JS变量
                        Regex("""href=["'][^"']*portal[^"']*id=(\d+)["']""")  // 链接模式
                    )
                    
                    for (pattern in patterns) {
                        val matches = pattern.findAll(mainPageBody)
                        matches.forEach { match ->
                            val portalId = match.groupValues[1]
                            if (portalId !in availablePortalIds) {
                                availablePortalIds.add(portalId)
                                Log.d(TAG, "Found Portal ID in main page: $portalId (pattern: ${pattern.pattern})")
                            }
                        }
                    }
                    
                    Log.d(TAG, "Portal IDs found in main page: $availablePortalIds")
                    
                    // 如果找到了Portal ID，直接測試這些ID的可用性
                    if (availablePortalIds.isNotEmpty()) {
                        Log.d(TAG, "Testing discovered Portal IDs for actual accessibility")
                        val testedPortalIds = mutableListOf<String>()
                        
                        for (portalId in availablePortalIds.toList()) {
                            if (testPortalIdAccess(portalId, baseUrl, sessionCookie)) {
                                testedPortalIds.add(portalId)
                                Log.d(TAG, "Confirmed accessible Portal ID: $portalId")
                            } else {
                                Log.d(TAG, "Portal ID $portalId is not accessible")
                            }
                        }
                        
                        if (testedPortalIds.isNotEmpty()) {
                            return@withContext testedPortalIds
                        }
                    }
                } else {
                    Log.w(TAG, "Failed to access Portal main page: ${mainPageResponse.code}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to access Portal main page: ${e.message}")
            }
            
            // 如果沒有從主頁找到Portal ID，或者所有發現的ID都不可訪問，嘗試常見的ID
            if (availablePortalIds.isEmpty()) {
                Log.d(TAG, "No accessible Portal IDs found in main page, testing common IDs: $COMMON_PORTAL_IDS")
                
                for (portalId in COMMON_PORTAL_IDS) {
                    if (testPortalIdAccess(portalId, baseUrl, sessionCookie)) {
                        availablePortalIds.add(portalId)
                        Log.d(TAG, "Found accessible Portal ID: $portalId")
                        
                        // 限制測試數量以避免過多請求
                        if (availablePortalIds.size >= 3) {
                            Log.d(TAG, "Found enough Portal IDs, stopping discovery")
                            break
                        }
                    }
                }
            }
            
            // 如果仍然沒有找到可用的Portal ID，作為最後的努力，返回一個空列表
            // 這將觸發AiChatApiService中的fallback邏輯
            if (availablePortalIds.isEmpty()) {
                Log.w(TAG, "No Portal IDs with POST permissions found")
                Log.i(TAG, "Note: Portal IDs 1-13 appear to have GET access only (based on discovery logs)")
                
                // 不要在這裡添加只讀ID，讓上層服務決定如何處理
            }
            
            Log.i(TAG, "Discovery complete. Available Portal IDs: $availablePortalIds")
            availablePortalIds
        }
    }
    
    /**
     * 測試特定Portal ID的訪問權限
     * 返回true表示可以完全訪問（包括POST），false表示無法訪問或只有只讀權限
     */
    private suspend fun testPortalIdAccess(portalId: String, baseUrl: String, sessionCookie: String): Boolean {
        return try {
            val testUrl = "$baseUrl/wise/wiseadm/s/promptportal/portal/form?id=$portalId"
            Log.d(TAG, "Testing Portal ID $portalId: $testUrl")
            
            // 首先使用 GET 請求檢查基本訪問權限
            val getResponse = httpClient.newCall(
                Request.Builder()
                    .url(testUrl)
                    .get()
                    .addHeader("Cookie", sessionCookie)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
            ).execute()
            
            Log.d(TAG, "Portal ID $portalId GET test response: ${getResponse.code}")
            if (!getResponse.isSuccessful || getResponse.code == 403) {
                Log.d(TAG, "Portal ID $portalId not accessible: ${getResponse.code}")
                return false
            }
            
            // 檢查回應內容，確保不是登入頁面
            val getResponseBody = getResponse.body?.string() ?: ""
            val isLoginPage = getResponseBody.contains("<title>智能客服 - 登入</title>") || 
                            getResponseBody.contains("login-form") ||
                            (getResponseBody.contains("loginName") && getResponseBody.contains("intumitPswd"))
            
            if (isLoginPage) {
                Log.d(TAG, "Portal ID $portalId redirected to login page")
                return false
            }
            
            // 檢查是否包含 Portal 表單元素
            val hasPortalForm = getResponseBody.contains("USERPROMPT") || 
                              getResponseBody.contains("portal") ||
                              getResponseBody.contains("form") ||
                              getResponseBody.contains("submit")
            
            if (!hasPortalForm) {
                Log.d(TAG, "Portal ID $portalId does not contain portal form elements")
                return false
            }
            
            // 進一步使用 POST 請求測試實際提交權限
            Log.d(TAG, "Portal ID $portalId passed GET test, testing POST submission")
            
            val testFormBody = FormBody.Builder()
                .add("USERPROMPT", "test")
                .add("USERUPLOADFILE", "data:application/octet-stream;base64,")
                .build()
            
            val postResponse = httpClient.newCall(
                Request.Builder()
                    .url(testUrl)
                    .post(testFormBody)
                    .addHeader("Cookie", sessionCookie)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
            ).execute()
            
            Log.d(TAG, "Portal ID $portalId POST test response: ${postResponse.code}")
            
            // 檢查POST響應
            when (postResponse.code) {
                200 -> {
                    // 成功響應，檢查是否有實際內容
                    val postResponseBody = postResponse.body?.string() ?: ""
                    if (postResponseBody.isNotEmpty() && !postResponseBody.contains("403") && !postResponseBody.contains("Forbidden")) {
                        Log.d(TAG, "Portal ID $portalId has full access (POST successful)")
                        return true
                    } else {
                        Log.d(TAG, "Portal ID $portalId POST returned empty or error response")
                        return false
                    }
                }
                400 -> {
                    // 400錯誤通常表示參數問題，但有權限，可以算作可訪問
                    Log.d(TAG, "Portal ID $portalId has submit permission (400 - parameter error)")
                    return true
                }
                403 -> {
                    Log.d(TAG, "Portal ID $portalId has GET access but no POST permission")
                    return false
                }
                else -> {
                    Log.d(TAG, "Portal ID $portalId POST test failed: ${postResponse.code}")
                    return false
                }
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "Error testing Portal ID $portalId: ${e.message}")
            false
        }
    }
}
