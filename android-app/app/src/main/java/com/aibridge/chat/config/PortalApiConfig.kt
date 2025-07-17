package com.aibridge.chat.config

/**
 * Portal API 專用配置
 * 
 * 這個配置文件包含了與 Wise Portal 系統交互的特定設置
 * 當需要切換到生產模式時，請參考這裡的配置
 */
object PortalApiConfig {
    
    /**
     * Portal 系統基礎 URL
     */
    const val PORTAL_BASE_URL = "https://dgb01p240102.japaneast.cloudapp.azure.com"
    
    /**
     * Portal 登錄路徑
     */
    const val PORTAL_LOGIN_PATH = "/wise/wiseadm/s/subadmin/2595af81-c151-47eb-9f15-d17e0adbe3b4/login"
    
    /**
     * Portal 訪問路徑  
     */
    const val PORTAL_ACCESS_PATH = "/wise/wiseadm/s/promptportal/portal"
    
    /**
     * 可能的聊天 API 端點（需要進一步調查）
     * 
     * 基於目前的測試，以下端點都返回 404：
     * - /api/chat
     * - /wise/wiseadm/s/promptportal/api/chat
     * 
     * 可能需要的端點：
     * - Portal 內的表單提交端點
     * - WebSocket 連接
     * - 或其他自定義 API 路徑
     */
    val POSSIBLE_CHAT_ENDPOINTS = listOf(
        "/api/chat",
        "/wise/api/chat", 
        "/wise/wiseadm/api/chat",
        "/wise/wiseadm/s/promptportal/chat",
        "/wise/wiseadm/s/promptportal/api/chat",
        "/chat",
        "/submit"
    )
    
    /**
     * Portal 表單 ID
     */
    const val DEFAULT_PORTAL_FORM_ID = "13"
    
    /**
     * 生產環境聊天 API URL（暫時不可用）
     * 
     * 注意：目前此端點返回 404
     * 需要進一步調查正確的 API 路徑
     */
    fun getProductionChatApiUrl(): String {
        return "$PORTAL_BASE_URL/api/chat"
    }
    
    /**
     * Portal 系統狀態檢查
     * 
     * 使用說明：
     * 1. Portal 登錄頁面可正常訪問：/wise/wiseadm/s/promptportal/portal 
     * 2. 需要先進行用戶認證
     * 3. 聊天功能可能需要不同的端點或實現方式
     */
    fun getPortalStatusInfo(): String {
        return """
        Portal 系統狀態：
        - 基礎 URL: $PORTAL_BASE_URL ✅ 可訪問
        - 登錄頁面: $PORTAL_BASE_URL$PORTAL_ACCESS_PATH ✅ 正常
        - 聊天 API: $PORTAL_BASE_URL/api/chat ❌ 404 錯誤
        
        需要調查：
        1. 正確的聊天 API 端點
        2. API 認證機制
        3. 請求格式和參數
        """.trimIndent()
    }
}
