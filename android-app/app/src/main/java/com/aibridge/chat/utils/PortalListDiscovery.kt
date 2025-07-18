package com.aibridge.chat.utils

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Portal 列表發現服務
 * 
 * 直接使用 Portal API 的 list 端點來獲取可用的 Portal 配置
 */
@Singleton
class PortalListDiscovery @Inject constructor(
    private val httpClient: OkHttpClient,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "PortalListDiscovery"
        private const val PORTAL_LIST_PATH = "/wise/wiseadm/s/promptportal/portal/list"
    }

    data class PortalListResponse(
        @SerializedName("content")
        val content: List<PortalItem> = emptyList(),
        @SerializedName("totalElements")
        val totalElements: Int = 0,
        @SerializedName("totalPages")
        val totalPages: Int = 0
    )

    data class PortalItem(
        @SerializedName("mgm")
        val mgm: PortalMgm
    )

    data class PortalMgm(
        @SerializedName("id")
        val id: Int,
        @SerializedName("tenantId")
        val tenantId: Int,
        @SerializedName("name")
        val name: String,
        @SerializedName("description")
        val description: String?,
        @SerializedName("model")
        val model: String?,
        @SerializedName("prompt")
        val prompt: String?,
        @SerializedName("status")
        val status: String,
        @SerializedName("inputSettings")
        val inputSettings: List<InputSetting> = emptyList()
    )

    data class InputSetting(
        @SerializedName("id")
        val id: Int,
        @SerializedName("required")
        val required: Boolean,
        @SerializedName("name")
        val name: String,
        @SerializedName("code")
        val code: String,
        @SerializedName("type")
        val type: String,
        @SerializedName("description")
        val description: String?,
        @SerializedName("maxLength")
        val maxLength: Int?,
        @SerializedName("noteForUser")
        val noteForUser: String?,
        @SerializedName("defaultValue")
        val defaultValue: String?,
        @SerializedName("fileExtensions")
        val fileExtensions: List<String> = emptyList(),
        @SerializedName("maxFileSize")
        val maxFileSize: Int?
    )

    data class DiscoveredPortals(
        val portals: List<PortalMgm>,
        val success: Boolean,
        val message: String
    )

    /**
     * 發現可用的 Portal 列表
     */
    suspend fun discoverPortals(baseUrl: String, sessionCookie: String): DiscoveredPortals = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Discovering Portal list from: $baseUrl")
            
            // 構建Portal list URL
            val listUrl = "$baseUrl$PORTAL_LIST_PATH?page=0&rows=20&sort=updatedTime&order=desc&term="
            Log.d(TAG, "Portal list URL: $listUrl")
            
            val request = Request.Builder()
                .url(listUrl)
                .get()
                .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
                .addHeader("Accept-Language", "zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-KL-KIS-Ajax-Request", "Ajax_Request")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("Cookie", sessionCookie)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            Log.d(TAG, "Making Portal list request...")
            val response = httpClient.newCall(request).execute()
            
            Log.d(TAG, "Portal list response code: ${response.code}")
            
            if (!response.isSuccessful) {
                Log.w(TAG, "Portal list request failed: ${response.code}")
                return@withContext DiscoveredPortals(
                    portals = emptyList(),
                    success = false,
                    message = "無法獲取Portal列表：HTTP ${response.code}"
                )
            }
            
            val responseBody = response.body?.string() ?: ""
            Log.d(TAG, "Portal list response length: ${responseBody.length}")
            Log.d(TAG, "Portal list response preview: ${responseBody.take(500)}")
            
            if (responseBody.isEmpty()) {
                Log.w(TAG, "Empty response from Portal list API")
                return@withContext DiscoveredPortals(
                    portals = emptyList(),
                    success = false,
                    message = "Portal列表回應為空"
                )
            }
            
            // 解析JSON回應
            try {
                val portalListResponse = gson.fromJson(responseBody, PortalListResponse::class.java)
                
                if (portalListResponse.content.isEmpty()) {
                    Log.w(TAG, "No portals found in response")
                    return@withContext DiscoveredPortals(
                        portals = emptyList(),
                        success = true,
                        message = "未找到可用的Portal"
                    )
                }
                
                val availablePortals = portalListResponse.content
                    .filter { it.mgm.status == "PUBLISH" }  // 只使用已發布的Portal
                    .map { it.mgm }
                
                Log.i(TAG, "Found ${availablePortals.size} published portals:")
                availablePortals.forEach { portal ->
                    Log.d(TAG, "Portal ${portal.id}: ${portal.name} (${portal.description})")
                }
                
                return@withContext DiscoveredPortals(
                    portals = availablePortals,
                    success = true,
                    message = "成功發現 ${availablePortals.size} 個可用Portal"
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse Portal list response: ${e.message}")
                Log.d(TAG, "Response body: $responseBody")
                return@withContext DiscoveredPortals(
                    portals = emptyList(),
                    success = false,
                    message = "解析Portal列表失敗：${e.message}"
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error discovering Portal list: ${e.message}", e)
            return@withContext DiscoveredPortals(
                portals = emptyList(),
                success = false,
                message = "發現Portal列表時發生錯誤：${e.message}"
            )
        }
    }

    /**
     * 檢查Portal是否可以訪問（簡單的GET請求測試）
     */
    suspend fun testPortalAccess(baseUrl: String, sessionCookie: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Testing Portal access to: $baseUrl")
            
            val portalUrl = "$baseUrl/wise/wiseadm/s/promptportal/portal"
            val request = Request.Builder()
                .url(portalUrl)
                .get()
                .addHeader("Cookie", sessionCookie)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = httpClient.newCall(request).execute()
            
            Log.d(TAG, "Portal access test response: ${response.code}")
            
            val hasAccess = response.isSuccessful && response.code != 302
            
            if (hasAccess) {
                val responseBody = response.body?.string() ?: ""
                val isLoginPage = responseBody.contains("login-form") || 
                                responseBody.contains("loginName") ||
                                responseBody.contains("智能客服 - 登入")
                
                if (isLoginPage) {
                    Log.w(TAG, "Portal access redirected to login page")
                    return@withContext false
                }
                
                Log.i(TAG, "Portal access test successful")
                return@withContext true
            } else {
                Log.w(TAG, "Portal access test failed: ${response.code}")
                return@withContext false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error testing Portal access: ${e.message}")
            return@withContext false
        }
    }
}
