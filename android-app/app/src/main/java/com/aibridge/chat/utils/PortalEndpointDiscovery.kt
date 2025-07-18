package com.aibridge.chat.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Portal 端點動態發現服務
 * 
 * 用於動態發現 Portal 系統的正確登入路徑，避免硬編碼 UUID
 */
@Singleton
class PortalEndpointDiscovery @Inject constructor(
    private val httpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "PortalEndpointDiscovery"
        
        // 可能的端點模式
        private val POSSIBLE_LOGIN_PATTERNS = listOf(
            "/wise/wiseadm/s/subadmin/{uuid}/login",
            "/wise/wiseadm/s/admin/{uuid}/login",
            "/wise/wiseadm/login",
            "/wise/admin/login",
            "/admin/login",
            "/login"
        )
        
        // 可能的 Portal 訪問模式
        private val POSSIBLE_PORTAL_PATTERNS = listOf(
            "/wise/wiseadm/s/promptportal/portal",
            "/wise/promptportal/portal",
            "/promptportal/portal",
            "/portal"
        )
    }
    
    data class DiscoveredEndpoints(
        val loginPath: String?,
        val portalPath: String?,
        val chatEndpoint: String?,
        val discoverySuccess: Boolean,
        val detectedUuid: String? = null
    )
    
    /**
     * 動態發現 Portal 端點
     */
    suspend fun discoverPortalEndpoints(baseUrl: String): DiscoveredEndpoints = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting Portal endpoint discovery for: $baseUrl")
        
        var loginPath: String? = null
        var portalPath: String? = null
        var chatEndpoint: String? = null
        var detectedUuid: String? = null
        
        try {
            // 1. 首先嘗試從主頁面發現重定向路徑
            Log.d(TAG, "Step 1: Attempting to discover paths via redirects")
            val discoveredPaths = discoverViaRedirects(baseUrl)
            if (discoveredPaths.isNotEmpty()) {
                Log.d(TAG, "Discovered paths via redirects: $discoveredPaths")
                loginPath = discoveredPaths["login"]
                portalPath = discoveredPaths["portal"]
                detectedUuid = extractUuidFromPath(loginPath ?: "")
            }
            
            // 2. 如果重定向發現失敗，嘗試掃描常見模式
            if (loginPath == null) {
                Log.d(TAG, "Step 2: Scanning common login patterns")
                loginPath = scanLoginPatterns(baseUrl)
                if (loginPath != null) {
                    detectedUuid = extractUuidFromPath(loginPath)
                    Log.d(TAG, "Found login path: $loginPath, detected UUID: $detectedUuid")
                }
            }
            
            // 3. 發現 Portal 訪問路徑
            if (portalPath == null) {
                Log.d(TAG, "Step 3: Scanning portal access patterns")
                portalPath = scanPortalPatterns(baseUrl)
                if (portalPath != null) {
                    Log.d(TAG, "Found portal path: $portalPath")
                }
            }
            
            // 4. 構建聊天端點
            if (portalPath != null) {
                chatEndpoint = "$portalPath/completion"
                Log.d(TAG, "Constructed chat endpoint: $chatEndpoint")
            }
            
            val success = loginPath != null && portalPath != null
            Log.i(TAG, "Discovery completed. Success: $success")
            
            DiscoveredEndpoints(
                loginPath = loginPath,
                portalPath = portalPath,
                chatEndpoint = chatEndpoint,
                discoverySuccess = success,
                detectedUuid = detectedUuid
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Portal endpoint discovery failed: ${e.message}", e)
            DiscoveredEndpoints(
                loginPath = null,
                portalPath = null,
                chatEndpoint = null,
                discoverySuccess = false
            )
        }
    }
    
    /**
     * 透過重定向發現端點
     */
    private suspend fun discoverViaRedirects(baseUrl: String): Map<String, String> {
        val discoveredPaths = mutableMapOf<String, String>()
        
        try {
            // 嘗試訪問基礎 URL，看是否有重定向
            val response = httpClient.newCall(
                Request.Builder()
                    .url(baseUrl)
                    .get()
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
            ).execute()
            
            // 檢查重定向位置
            response.header("Location")?.let { location ->
                Log.d(TAG, "Found redirect location: $location")
                if (location.contains("/login")) {
                    discoveredPaths["login"] = location.substringAfter(baseUrl).ifEmpty { location }
                }
            }
            
            // 檢查回應內容中的連結
            val responseBody = response.body?.string() ?: ""
            if (responseBody.isNotEmpty()) {
                extractLinksFromHtml(responseBody, discoveredPaths)
            }
            
            response.close()
            
        } catch (e: Exception) {
            Log.w(TAG, "Redirect discovery failed: ${e.message}")
        }
        
        return discoveredPaths
    }
    
    /**
     * 掃描常見的登入模式
     */
    private suspend fun scanLoginPatterns(baseUrl: String): String? {
        // 首先嘗試發現 UUID
        val possibleUuids = discoverUuids(baseUrl)
        
        for (uuid in possibleUuids) {
            for (pattern in POSSIBLE_LOGIN_PATTERNS) {
                val path = pattern.replace("{uuid}", uuid)
                if (testEndpoint(baseUrl + path)) {
                    Log.d(TAG, "Found working login path: $path with UUID: $uuid")
                    return path
                }
            }
        }
        
        // 如果沒有找到 UUID，嘗試不需要 UUID 的模式
        for (pattern in POSSIBLE_LOGIN_PATTERNS) {
            if (!pattern.contains("{uuid}")) {
                if (testEndpoint(baseUrl + pattern)) {
                    Log.d(TAG, "Found working login path: $pattern")
                    return pattern
                }
            }
        }
        
        return null
    }
    
    /**
     * 掃描 Portal 訪問模式
     */
    private suspend fun scanPortalPatterns(baseUrl: String): String? {
        for (pattern in POSSIBLE_PORTAL_PATTERNS) {
            if (testEndpoint(baseUrl + pattern)) {
                Log.d(TAG, "Found working portal path: $pattern")
                return pattern
            }
        }
        return null
    }
    
    /**
     * 發現可能的 UUID
     */
    private suspend fun discoverUuids(baseUrl: String): List<String> {
        val uuids = mutableListOf<String>()
        
        try {
            // 嘗試訪問一些可能包含 UUID 的端點
            val possiblePaths = listOf(
                "/wise/wiseadm/s/",
                "/wise/admin/",
                "/admin/"
            )
            
            for (path in possiblePaths) {
                try {
                    val response = httpClient.newCall(
                        Request.Builder()
                            .url(baseUrl + path)
                            .get()
                            .build()
                    ).execute()
                    
                    val responseBody = response.body?.string() ?: ""
                    val foundUuids = extractUuidsFromContent(responseBody)
                    uuids.addAll(foundUuids)
                    
                    response.close()
                } catch (e: Exception) {
                    // 忽略個別請求失敗
                }
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "UUID discovery failed: ${e.message}")
        }
        
        // 如果沒有發現 UUID，使用已知的作為後備
        if (uuids.isEmpty()) {
            uuids.add("2595af81-c151-47eb-9f15-d17e0adbe3b4") // 當前已知的 UUID
        }
        
        return uuids.distinct()
    }
    
    /**
     * 測試端點是否可訪問
     */
    private suspend fun testEndpoint(url: String): Boolean {
        return try {
            val response = httpClient.newCall(
                Request.Builder()
                    .url(url)
                    .head()
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
            ).execute()
            
            val success = response.isSuccessful || response.code == 302
            response.close()
            success
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 從 HTML 內容中提取連結
     */
    private fun extractLinksFromHtml(html: String, discoveredPaths: MutableMap<String, String>) {
        // 尋找 action 屬性中的登入路徑
        val actionRegex = """action\s*=\s*["']([^"']*login[^"']*)["']""".toRegex(RegexOption.IGNORE_CASE)
        actionRegex.findAll(html).forEach { match ->
            val path = match.groupValues[1]
            Log.d(TAG, "Found login action path: $path")
            discoveredPaths["login"] = path
        }
        
        // 尋找 href 屬性中的 portal 路徑
        val hrefRegex = """href\s*=\s*["']([^"']*portal[^"']*)["']""".toRegex(RegexOption.IGNORE_CASE)
        hrefRegex.findAll(html).forEach { match ->
            val path = match.groupValues[1]
            Log.d(TAG, "Found portal href path: $path")
            discoveredPaths["portal"] = path
        }
    }
    
    /**
     * 從內容中提取 UUID
     */
    private fun extractUuidsFromContent(content: String): List<String> {
        val uuidRegex = """[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}""".toRegex()
        return uuidRegex.findAll(content).map { it.value }.toList()
    }
    
    /**
     * 從路徑中提取 UUID
     */
    private fun extractUuidFromPath(path: String): String? {
        val uuidRegex = """[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}""".toRegex()
        return uuidRegex.find(path)?.value
    }
    
    /**
     * 獲取發現結果的摘要
     */
    fun getDiscoverySummary(endpoints: DiscoveredEndpoints): String {
        return buildString {
            appendLine("=== Portal 端點發現結果 ===")
            appendLine("發現狀態: ${if (endpoints.discoverySuccess) "✅ 成功" else "❌ 失敗"}")
            appendLine()
            appendLine("發現的端點:")
            appendLine("  登入路徑: ${endpoints.loginPath ?: "未發現"}")
            appendLine("  Portal路徑: ${endpoints.portalPath ?: "未發現"}")
            appendLine("  聊天端點: ${endpoints.chatEndpoint ?: "未發現"}")
            appendLine("  檢測到的UUID: ${endpoints.detectedUuid ?: "無"}")
        }
    }
}
