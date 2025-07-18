package com.aibridge.chat.config

/**
 * 後端 API 配置
 * 根據後端說明文檔配置
 */
object ApiConfig {
    
    /**
     * 動態端點配置
     */
    private var dynamicPortalId: String? = null
    private var dynamicLoginUrl: String? = null
    private var dynamicPortalCompletionUrl: String? = null
    
    /**
     * 運行模式配置
     */
    enum class RunMode {
        TEST,           // 測試模式：使用 httpbin.org
        DEVELOPMENT,    // 開發模式：使用本地後端
        PRODUCTION      // 生產模式：使用實際後端服務
    }
    
    // 當前運行模式
    private const val CURRENT_MODE = "PRODUCTION"  // 可改為 "DEVELOPMENT" 或 "PRODUCTION"
    
    private fun getRunMode(): RunMode {
        return when (CURRENT_MODE) {
            "DEVELOPMENT" -> RunMode.DEVELOPMENT
            "PRODUCTION" -> RunMode.PRODUCTION
            else -> RunMode.TEST
        }
    }
    
    /**
     * 公開的getCurrentMode方法
     */
    fun getCurrentMode(): RunMode = getRunMode()
    
    /**
     * 根據模式獲取基礎 URL
     */
    val BACKEND_BASE_URL: String
        get() = when (getRunMode()) {
            RunMode.TEST -> "https://httpbin.org"
            RunMode.DEVELOPMENT -> "http://localhost:3000"  // 本地開發服務器
            RunMode.PRODUCTION -> "https://dgb01p240102.japaneast.cloudapp.azure.com"  // 實際後端服務
        }
    
    /**
     * 根據模式獲取聊天端點
     */
    val CHAT_ENDPOINT: String
        get() = when (getRunMode()) {
            RunMode.TEST -> "/post"
            RunMode.DEVELOPMENT -> "/api/chat"
            RunMode.PRODUCTION -> "/wise/wiseadm/s/promptportal/portal/completion"
        }
    
    /**
     * 登入檢查端點（可選）
     */
    const val CHECK_LOGIN_ENDPOINT = "/api/check-login"
    
    /**
     * 存取權限檢查端點（可選）
     */
    const val CHECK_ACCESS_ENDPOINT = "/api/check-access"
    
    /**
     * 預設的 Portal 表單 ID
     */
    const val DEFAULT_PORTAL_ID = "13"
    
    /**
     * 設定動態端點資訊
     */
    fun setDynamicEndpoints(portalId: String?, loginUrl: String?, completionUrl: String?) {
        dynamicPortalId = portalId
        dynamicLoginUrl = loginUrl
        dynamicPortalCompletionUrl = completionUrl
    }
    
    /**
     * 獲取當前Portal ID (優先使用動態發現的)
     */
    fun getCurrentPortalId(): String = dynamicPortalId ?: DEFAULT_PORTAL_ID
    
    /**
     * 獲取登入URL (優先使用動態發現的)
     */
    fun getLoginUrl(): String? = dynamicLoginUrl
    
    /**
     * 獲取Portal完成端點URL (優先使用動態發現的)
     */
    fun getPortalCompletionUrl(): String? = dynamicPortalCompletionUrl
    
    /**
     * 獲取完整的聊天 API URL (優先使用動態發現的端點)
     */
    fun getChatApiUrl(): String = dynamicPortalCompletionUrl ?: "$BACKEND_BASE_URL$CHAT_ENDPOINT"
    
    /**
     * 獲取完整的登入檢查 API URL
     */
    fun getCheckLoginApiUrl(): String = "$BACKEND_BASE_URL$CHECK_LOGIN_ENDPOINT"
    
    /**
     * 獲取完整的存取權限檢查 API URL  
     */
    fun getCheckAccessApiUrl(): String = "$BACKEND_BASE_URL$CHECK_ACCESS_ENDPOINT"
    
    /**
     * 獲取當前模式的顯示名稱
     */
    fun getCurrentModeDisplayName(): String {
        return when (getRunMode()) {
            RunMode.TEST -> "🧪 測試模式"
            RunMode.DEVELOPMENT -> "🔨 開發模式"
            RunMode.PRODUCTION -> "🚀 生產模式"
        }
    }
    
    /**
     * 檢查是否為測試模式
     */
    fun isTestMode(): Boolean = getRunMode() == RunMode.TEST
    
    /**
     * 獲取模式說明文字
     */
    fun getModeDescription(): String {
        return when (getRunMode()) {
            RunMode.TEST -> "使用 httpbin.org 進行網路連線測試，不會連接到真實 AI 服務"
            RunMode.DEVELOPMENT -> "連接到本地開發服務器 (localhost:3000)"
            RunMode.PRODUCTION -> "連接到實際的 AI Bridge 後端服務"
        }
    }
}
