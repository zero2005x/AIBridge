package com.aibridge.chat.data.api

import android.util.Log
import com.aibridge.chat.config.ApiConfig
import com.aibridge.chat.domain.model.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortalDiscoveryService @Inject constructor(
    private val httpClient: OkHttpClient,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "PortalDiscoveryService"
    }

    /**
     * 發現可用的Portal
     */
    suspend fun discoverPortals(): List<PortalDetail> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting portal discovery")
            
            // 首先獲取Portal列表頁面
            val portalListUrl = "${ApiConfig.BACKEND_BASE_URL}/wise/wiseadm/s/promptportal/prompt"
            val portalList = fetchPortalList(portalListUrl)
            
            Log.d(TAG, "Discovered ${portalList.size} portals from list page")
            
            // 獲取每個Portal的詳細信息
            val detailedPortals = mutableListOf<PortalDetail>()
            portalList.forEach { portal ->
                try {
                    val detail = fetchPortalDetail(portal.id)
                    if (detail != null) {
                        detailedPortals.add(detail)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to get details for portal ${portal.id}: ${e.message}")
                }
            }
            
            Log.d(TAG, "Successfully fetched details for ${detailedPortals.size} portals")
            detailedPortals
        } catch (e: Exception) {
            Log.e(TAG, "Failed to discover portals: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * 從Portal列表頁面獲取Portal列表
     */
    private suspend fun fetchPortalList(url: String): List<PortalDetail> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.w(TAG, "Failed to fetch portal list: ${response.code}")
                return@withContext emptyList()
            }

            val html = response.body?.string() ?: ""
            parsePortalListFromHtml(html)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching portal list: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * 解析HTML頁面獲取Portal信息
     */
    private fun parsePortalListFromHtml(html: String): List<PortalDetail> {
        val portals = mutableListOf<PortalDetail>()
        
        try {
            // 尋找Portal鏈接的模式，例如：/wise/wiseadm/s/promptportal/portal/form?id=13
            val portalLinkPattern = Pattern.compile(
                """/wise/wiseadm/s/promptportal/portal/form\?id=(\d+)""",
                Pattern.CASE_INSENSITIVE
            )
            
            val matcher = portalLinkPattern.matcher(html)
            val foundIds = mutableSetOf<String>()
            
            while (matcher.find()) {
                val portalId = matcher.group(1)
                if (foundIds.add(portalId)) {
                    portals.add(
                        PortalDetail(
                            id = portalId,
                            name = "Portal $portalId",
                            description = "自動發現的Portal",
                            isAccessible = true,
                            lastUpdated = System.currentTimeMillis()
                        )
                    )
                }
            }
            
            // 也搜索表格或列表中的Portal信息
            val tableRowPattern = Pattern.compile(
                """<tr[^>]*>.*?Portal\s*(\d+).*?</tr>""",
                Pattern.CASE_INSENSITIVE or Pattern.DOTALL
            )
            
            val tableMatches = tableRowPattern.matcher(html)
            while (tableMatches.find()) {
                val portalId = tableMatches.group(1)
                if (foundIds.add(portalId)) {
                    // 嘗試提取更多信息
                    val rowHtml = tableMatches.group(0)
                    val name = extractPortalName(rowHtml, portalId)
                    val description = extractPortalDescription(rowHtml)
                    
                    portals.add(
                        PortalDetail(
                            id = portalId,
                            name = name,
                            description = description,
                            isAccessible = true,
                            lastUpdated = System.currentTimeMillis()
                        )
                    )
                }
            }
            
            Log.d(TAG, "Parsed ${portals.size} portals from HTML")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing portal list from HTML: ${e.message}", e)
        }
        
        return portals
    }

    /**
     * 獲取Portal的詳細信息
     */
    private suspend fun fetchPortalDetail(portalId: String): PortalDetail? = withContext(Dispatchers.IO) {
        try {
            val portalFormUrl = "${ApiConfig.BACKEND_BASE_URL}/wise/wiseadm/s/promptportal/portal/form?id=$portalId"
            
            val request = Request.Builder()
                .url(portalFormUrl)
                .get()
                .addHeader("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.w(TAG, "Failed to fetch portal detail for $portalId: ${response.code}")
                return@withContext null
            }

            val html = response.body?.string() ?: ""
            parsePortalDetailFromHtml(portalId, html)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching portal detail for $portalId: ${e.message}", e)
            null
        }
    }

    /**
     * 從Portal表單頁面解析詳細信息
     */
    private fun parsePortalDetailFromHtml(portalId: String, html: String): PortalDetail {
        try {
            // 提取頁面標題作為Portal名稱
            val titlePattern = Pattern.compile("<title[^>]*>([^<]+)</title>", Pattern.CASE_INSENSITIVE)
            val titleMatcher = titlePattern.matcher(html)
            val name = if (titleMatcher.find()) {
                titleMatcher.group(1).trim()
            } else {
                "Portal $portalId"
            }

            // 提取表單字段作為參數定義
            val parameters = extractFormParameters(html)
            
            // 提取描述信息
            val description = extractDescription(html)
            
            return PortalDetail(
                id = portalId,
                name = name,
                description = description,
                parameters = parameters,
                isAccessible = true,
                lastUpdated = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing portal detail for $portalId: ${e.message}", e)
            return PortalDetail(
                id = portalId,
                name = "Portal $portalId",
                description = "解析失敗",
                isAccessible = true,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    /**
     * 從HTML表單中提取參數定義
     */
    private fun extractFormParameters(html: String): List<ParameterDefinition> {
        val parameters = mutableListOf<ParameterDefinition>()
        
        try {
            // 查找input和textarea字段
            val inputPattern = Pattern.compile(
                """<(input|textarea)[^>]*name=['"]([^'"]+)['"][^>]*>""",
                Pattern.CASE_INSENSITIVE
            )
            
            val matcher = inputPattern.matcher(html)
            while (matcher.find()) {
                val elementType = matcher.group(1).lowercase()
                val name = matcher.group(2)
                val fullTag = matcher.group(0)
                
                // 跳過隱藏字段和系統字段
                if (fullTag.contains("type=\"hidden\"", true) || 
                    name.startsWith("_") || 
                    name.lowercase() in listOf("csrf", "token")) {
                    continue
                }
                
                val paramType = when {
                    fullTag.contains("type=\"file\"", true) -> ParameterType.FILE
                    elementType == "textarea" -> ParameterType.TEXTAREA
                    fullTag.contains("type=\"number\"", true) -> ParameterType.NUMBER
                    else -> ParameterType.TEXT
                }
                
                val isRequired = fullTag.contains("required", true)
                val placeholder = extractAttribute(fullTag, "placeholder")
                
                parameters.add(
                    ParameterDefinition(
                        name = name,
                        type = paramType,
                        isRequired = isRequired,
                        description = generateParameterDescription(name),
                        placeholder = placeholder
                    )
                )
            }
            
            // 如果沒有找到參數，添加默認參數
            if (parameters.isEmpty()) {
                parameters.addAll(getDefaultParameters())
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting form parameters: ${e.message}", e)
            return getDefaultParameters()
        }
        
        return parameters
    }

    private fun extractAttribute(html: String, attributeName: String): String {
        val pattern = Pattern.compile(
            """$attributeName=['"]([^'"]+)['"]""",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = pattern.matcher(html)
        return if (matcher.find()) matcher.group(1) else ""
    }

    private fun extractPortalName(html: String, portalId: String): String {
        // 嘗試提取更具描述性的名稱
        val patterns = listOf(
            """<td[^>]*>([^<]*Portal[^<]*)</td>""",
            """<span[^>]*>([^<]*Portal[^<]*)</span>""",
            """<div[^>]*>([^<]*Portal[^<]*)</div>"""
        )
        
        for (pattern in patterns) {
            val matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(html)
            if (matcher.find()) {
                val name = matcher.group(1).trim()
                if (name.isNotEmpty() && name != "Portal") {
                    return name
                }
            }
        }
        
        return "Portal $portalId"
    }

    private fun extractPortalDescription(html: String): String {
        // 嘗試提取描述信息
        val patterns = listOf(
            """<td[^>]*>([^<]*說明[^<]*)</td>""",
            """<td[^>]*>([^<]*描述[^<]*)</td>""",
            """<span[^>]*>([^<]*說明[^<]*)</span>"""
        )
        
        for (pattern in patterns) {
            val matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(html)
            if (matcher.find()) {
                val desc = matcher.group(1).trim()
                if (desc.isNotEmpty()) {
                    return desc
                }
            }
        }
        
        return "自動發現的Portal"
    }

    private fun extractDescription(html: String): String {
        // 從meta description或其他地方提取描述
        val metaPattern = Pattern.compile(
            """<meta[^>]*name=['"]description['"][^>]*content=['"]([^'"]+)['"]""",
            Pattern.CASE_INSENSITIVE
        )
        val metaMatcher = metaPattern.matcher(html)
        if (metaMatcher.find()) {
            return metaMatcher.group(1)
        }
        
        return "AI Portal服務"
    }

    private fun generateParameterDescription(paramName: String): String {
        return when (paramName.uppercase()) {
            "USERPROMPT" -> "用戶輸入的提示文字或問題"
            "USERUPLOADFILE" -> "用戶上傳的文件內容（Base64編碼）"
            "TEMPERATURE" -> "控制回應的創造性（0.0-1.0）"
            "MAX_TOKENS" -> "最大回應長度"
            "MODEL" -> "使用的AI模型"
            else -> "自定義參數：$paramName"
        }
    }

    private fun getDefaultParameters(): List<ParameterDefinition> {
        return listOf(
            ParameterDefinition(
                name = "USERPROMPT",
                type = ParameterType.TEXTAREA,
                isRequired = true,
                description = "用戶輸入的提示文字或問題",
                placeholder = "請輸入您的問題或要求..."
            ),
            ParameterDefinition(
                name = "USERUPLOADFILE",
                type = ParameterType.FILE,
                isRequired = false,
                description = "用戶上傳的文件內容（Base64編碼）",
                placeholder = "選擇要上傳的文件..."
            )
        )
    }
}
