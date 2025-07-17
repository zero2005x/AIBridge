package com.aibridge.chat.utils

import android.util.Log
import com.aibridge.chat.data.api.AuthApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerConnectionTester @Inject constructor(
    private val httpClient: OkHttpClient,
    private val authApiService: AuthApiService,
    private val networkUtils: NetworkUtils
) {
    companion object {
        private const val TAG = "ServerConnectionTester"
        private const val TEST_BASE_URL = "https://dgb01p240102.japaneast.cloudapp.azure.com"
    }
    
    data class ConnectionTestResult(
        val success: Boolean,
        val message: String,
        val details: Map<String, Any>
    )
    
    suspend fun runFullConnectionTest(
        baseUrl: String = TEST_BASE_URL,
        testUsername: String? = null,
        testPassword: String? = null
    ): ConnectionTestResult = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting full connection test for: $baseUrl")
        
        val testResults = mutableMapOf<String, Any>()
        val errors = mutableListOf<String>()
        
        try {
            // 1. 基本網路連線檢查
            Log.d(TAG, "Step 1: Basic network connectivity")
            val networkAvailable = networkUtils.isNetworkAvailable()
            testResults["network_available"] = networkAvailable
            
            if (!networkAvailable) {
                errors.add("網路連線不可用")
                return@withContext ConnectionTestResult(false, "網路連線檢查失敗", testResults)
            }
            
            // 2. DNS 解析檢查
            Log.d(TAG, "Step 2: DNS resolution test")
            val canResolveHost = networkUtils.canResolveHost(baseUrl)
            testResults["dns_resolution"] = canResolveHost
            
            if (!canResolveHost) {
                errors.add("無法解析主機名稱")
                return@withContext ConnectionTestResult(false, "DNS 解析失敗", testResults)
            }
            
            // 3. 基本 HTTP 連線測試
            Log.d(TAG, "Step 3: Basic HTTP connectivity")
            val httpConnectivity = testHttpConnectivity(baseUrl)
            testResults["http_connectivity"] = httpConnectivity
            
            if (!httpConnectivity) {
                errors.add("HTTP 連線失敗")
            }
            
            // 4. 登入頁面測試
            Log.d(TAG, "Step 4: Login page access test")
            val loginPageAccess = testLoginPageAccess(baseUrl)
            testResults["login_page_access"] = loginPageAccess
            
            if (!loginPageAccess) {
                errors.add("無法訪問登入頁面")
            }
            
            // 5. 如果提供了測試憑證，測試登入流程
            if (testUsername != null && testPassword != null) {
                Log.d(TAG, "Step 5: Testing login flow")
                val loginTest = testLoginFlow(baseUrl, testUsername, testPassword)
                testResults["login_flow"] = loginTest
                
                if (!loginTest) {
                    errors.add("登入流程測試失敗")
                }
            }
            
            val success = errors.isEmpty()
            val message = if (success) {
                "所有連線測試通過"
            } else {
                "測試失敗: ${errors.joinToString(", ")}"
            }
            
            ConnectionTestResult(success, message, testResults)
            
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed: ${e.message}", e)
            errors.add("測試過程發生異常: ${e.message}")
            ConnectionTestResult(false, "連線測試異常", testResults)
        }
    }
    
    private suspend fun testHttpConnectivity(baseUrl: String): Boolean {
        return try {
            val response = httpClient.newCall(
                Request.Builder()
                    .url(baseUrl)
                    .head() // 使用 HEAD 請求避免下載內容
                    .addHeader("User-Agent", "AIBridge-ConnectionTest/1.0")
                    .build()
            ).execute()
            
            val success = response.isSuccessful || response.code in 300..399
            response.close()
            
            Log.d(TAG, "HTTP connectivity test: $success (code: ${response.code})")
            success
        } catch (e: Exception) {
            Log.w(TAG, "HTTP connectivity test failed: ${e.message}")
            false
        }
    }
    
    private suspend fun testLoginPageAccess(baseUrl: String): Boolean {
        return try {
            val loginUrl = "$baseUrl/wise/wiseadm/s/subadmin/2595af81-c151-47eb-9f15-d17e0adbe3b4/login"
            
            val response = httpClient.newCall(
                Request.Builder()
                    .url(loginUrl)
                    .get()
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .build()
            ).execute()
            
            val success = response.isSuccessful
            response.close()
            
            Log.d(TAG, "Login page access test: $success (code: ${response.code})")
            success
        } catch (e: Exception) {
            Log.w(TAG, "Login page access test failed: ${e.message}")
            false
        }
    }
    
    private suspend fun testLoginFlow(baseUrl: String, username: String, password: String): Boolean {
        return try {
            val result = authApiService.checkLogin(username, password, baseUrl)
            Log.d(TAG, "Login flow test result: ${result.success}")
            result.success
        } catch (e: Exception) {
            Log.w(TAG, "Login flow test failed: ${e.message}")
            false
        }
    }
    
    fun getTestSummary(result: ConnectionTestResult): String {
        val builder = StringBuilder()
        builder.append("=== 伺服器連線測試報告 ===\n")
        builder.append("整體結果: ${if (result.success) "✅ 成功" else "❌ 失敗"}\n")
        builder.append("訊息: ${result.message}\n\n")
        
        builder.append("詳細測試結果:\n")
        result.details.forEach { (key, value) ->
            val status = when (value) {
                true -> "✅ 通過"
                false -> "❌ 失敗"
                else -> value.toString()
            }
            builder.append("  $key: $status\n")
        }
        
        return builder.toString()
    }
}
