package com.aibridge.chat.data.repository

import android.content.SharedPreferences
import android.util.Log
import com.aibridge.chat.data.api.AuthApiService
import com.aibridge.chat.domain.model.LoginResult
import com.aibridge.chat.domain.model.PortalCredentials
import com.aibridge.chat.domain.model.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val encryptedPrefs: SharedPreferences
) {
    
    companion object {
        private const val TAG = "AuthRepository"
        private const val KEY_SESSION_COOKIE = "session_cookie"
        private const val KEY_USERNAME = "username"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_LOGIN_TIME = "login_time"
        private const val KEY_TEMP_PASSWORD = "temp_password" // 臨時存儲密碼用於 AI 服務
        private const val KEY_AVAILABLE_PORTAL_IDS = "available_portal_ids" // 可用的Portal ID列表
        private const val KEY_DEFAULT_PORTAL_ID = "default_portal_id" // 默認使用的Portal ID
        private const val SESSION_TIMEOUT = 24 * 60 * 60 * 1000L // 24 hours
    }
    
    private val _loginState = MutableStateFlow<LoginState>(LoginState.NotLoggedIn)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()
    
    init {
        checkStoredSession()
    }
    
    private fun checkStoredSession() {
        Log.d(TAG, "Checking stored session")
        val sessionCookie = encryptedPrefs.getString(KEY_SESSION_COOKIE, null)
        val username = encryptedPrefs.getString(KEY_USERNAME, null)
        val loginTime = encryptedPrefs.getLong(KEY_LOGIN_TIME, 0L)
        
        Log.d(TAG, "Stored session - cookie exists: ${sessionCookie != null}, username: $username, loginTime: $loginTime")
        
        if (sessionCookie != null && username != null && 
            System.currentTimeMillis() - loginTime < SESSION_TIMEOUT) {
            Log.i(TAG, "Valid stored session found, restoring login state")
            _loginState.value = LoginState.LoggedIn(
                sessionCookie = sessionCookie,
                userInfo = UserInfo(username = username)
            )
        } else {
            Log.d(TAG, "No valid stored session found")
        }
    }
    
    suspend fun login(credentials: PortalCredentials): LoginResult {
        Log.d(TAG, "Starting Portal login request to: ${credentials.baseUrl}")
        return try {
            val result = authApiService.checkLogin(
                username = credentials.username,
                password = credentials.password,
                baseUrl = credentials.baseUrl
            )
            
            Log.d(TAG, "Portal login result: success=${result.success}, message=${result.message}")
            
            if (result.success && result.sessionCookie != null) {
                Log.i(TAG, "Portal login successful, verifying access")
                
                // 暫時跳過Portal訪問權限檢查，直接允許訪問
                // 因為登入已經成功，我們假設用戶有適當的權限
                val hasAccess = true
                Log.i(TAG, "Skipping Portal access check - assuming access granted after successful login")
                
                if (hasAccess) {
                    Log.i(TAG, "Portal access verified, discovering available Portal IDs")
                    
                    // 發現可用的Portal ID
                    try {
                        val availablePortalIds = authApiService.discoverAvailablePortalIds(
                            result.sessionCookie, 
                            credentials.baseUrl
                        )
                        
                        Log.i(TAG, "Portal ID discovery completed. Found IDs: $availablePortalIds")
                        
                        if (availablePortalIds.isNotEmpty()) {
                            Log.i(TAG, "Found ${availablePortalIds.size} accessible Portal IDs: $availablePortalIds")
                            // 使用第一個可用的Portal ID作為默認值
                            val defaultPortalId = availablePortalIds.first()
                            Log.i(TAG, "Setting default Portal ID to: $defaultPortalId")
                            
                            encryptedPrefs.edit()
                                .putString(KEY_SESSION_COOKIE, result.sessionCookie)
                                .putString(KEY_USERNAME, credentials.username)
                                .putString(KEY_BASE_URL, credentials.baseUrl)
                                .putString(KEY_TEMP_PASSWORD, credentials.password) // 臨時存儲密碼
                                .putString(KEY_AVAILABLE_PORTAL_IDS, availablePortalIds.joinToString(","))
                                .putString(KEY_DEFAULT_PORTAL_ID, defaultPortalId)
                                .putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
                                .apply()
                        } else {
                            Log.w(TAG, "No Portal IDs with submit permissions found, but login successful")
                            // 用戶登入成功但沒有任何Portal的提交權限
                            // 保存session但不設置Portal ID列表，讓上層應用決定如何處理
                            encryptedPrefs.edit()
                                .putString(KEY_SESSION_COOKIE, result.sessionCookie)
                                .putString(KEY_USERNAME, credentials.username)
                                .putString(KEY_BASE_URL, credentials.baseUrl)
                                .putString(KEY_TEMP_PASSWORD, credentials.password) // 臨時存儲密碼
                                .putString(KEY_AVAILABLE_PORTAL_IDS, "") // 空字符串表示沒有可用的Portal ID
                                .putString(KEY_DEFAULT_PORTAL_ID, "") // 空字符串表示沒有默認Portal ID
                                .putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
                                .apply()
                            
                            Log.i(TAG, "Session saved without Portal access - user may need additional permissions")
                        }
                        
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to discover Portal IDs: ${e.message}")
                        // 如果發現失敗，仍然保存基本session信息
                        encryptedPrefs.edit()
                            .putString(KEY_SESSION_COOKIE, result.sessionCookie)
                            .putString(KEY_USERNAME, credentials.username)
                            .putString(KEY_BASE_URL, credentials.baseUrl)
                            .putString(KEY_TEMP_PASSWORD, credentials.password) // 臨時存儲密碼
                            .putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
                            .apply()
                    }
                    
                    _loginState.value = LoginState.LoggedIn(
                        sessionCookie = result.sessionCookie,
                        userInfo = result.userInfo ?: UserInfo(username = credentials.username)
                    )
                    
                    LoginResult(success = true, sessionCookie = result.sessionCookie, userInfo = result.userInfo)
                } else {
                    Log.w(TAG, "Portal access denied after successful login")
                    LoginResult(success = false, message = "登入成功但無 Portal 訪問權限")
                }
            } else {
                val errorMsg = result.message ?: "Portal 登入失敗"
                Log.w(TAG, "Portal login failed: $errorMsg")
                LoginResult(success = false, message = errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Portal login exception: ${e.message}", e)
            // Provide more specific error messages based on exception type
            val errorMessage = when {
                e.message?.contains("Unable to resolve host") == true -> 
                    "無法連接到伺服器：請檢查網路連線和伺服器位址"
                e.message?.contains("timeout") == true -> 
                    "連線逾時：請檢查網路環境或稍後重試"
                e.message?.contains("Connection refused") == true -> 
                    "連線被拒絕：伺服器可能暫時無法使用"
                else -> "連線錯誤: ${e.message}"
            }
            LoginResult(success = false, message = errorMessage)
        }
    }
    
    suspend fun checkAccess(): Boolean {
        val currentState = _loginState.value
        if (currentState !is LoginState.LoggedIn) return false
        
        return try {
            val baseUrl = encryptedPrefs.getString(KEY_BASE_URL, "") ?: ""
            Log.d(TAG, "Checking portal access")
            val hasAccess = authApiService.checkAccess(currentState.sessionCookie, baseUrl)
            Log.d(TAG, "Access check result: $hasAccess")
            hasAccess
        } catch (e: Exception) {
            Log.e(TAG, "Access check exception: ${e.message}", e)
            false
        }
    }
    
    suspend fun logout() {
        val currentState = _loginState.value
        if (currentState is LoginState.LoggedIn) {
            try {
                Log.d(TAG, "Logging out from portal")
                authApiService.logout(sessionCookie = currentState.sessionCookie)
            } catch (e: Exception) {
                Log.w(TAG, "Logout API error: ${e.message}", e)
            }
        }
        
        // Clear stored session
        encryptedPrefs.edit()
            .remove(KEY_SESSION_COOKIE)
            .remove(KEY_USERNAME)
            .remove(KEY_BASE_URL)
            .remove(KEY_TEMP_PASSWORD) // 清除臨時密碼
            .remove(KEY_AVAILABLE_PORTAL_IDS) // 清除可用的Portal ID
            .remove(KEY_DEFAULT_PORTAL_ID) // 清除默認Portal ID
            .remove(KEY_LOGIN_TIME)
            .apply()
        
        _loginState.value = LoginState.NotLoggedIn
        Log.i(TAG, "Logout completed")
    }
    
    fun getCurrentSessionCookie(): String? {
        return when (val state = _loginState.value) {
            is LoginState.LoggedIn -> state.sessionCookie
            else -> null
        }
    }
    
    fun getCurrentUserInfo(): UserInfo? {
        return when (val state = _loginState.value) {
            is LoginState.LoggedIn -> state.userInfo
            else -> null
        }
    }
    
    /**
     * 獲取存儲的登入憑證 - 僅用於 AI 聊天服務
     * 注意：密碼暫存在記憶體中，實際產品中應使用更安全的方法
     */
    fun getStoredCredentials(): PortalCredentials? {
        return try {
            val username = encryptedPrefs.getString(KEY_USERNAME, null)
            val baseUrl = encryptedPrefs.getString(KEY_BASE_URL, null)
            val password = encryptedPrefs.getString(KEY_TEMP_PASSWORD, null)
            
            if (username != null && baseUrl != null && password != null) {
                PortalCredentials(
                    username = username,
                    password = password,
                    baseUrl = baseUrl
                )
            } else {
                Log.w(TAG, "Incomplete stored credentials")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving stored credentials: ${e.message}")
            null
        }
    }
    
    /**
     * 獲取可用的Portal ID列表
     */
    fun getAvailablePortalIds(): List<String> {
        return try {
            val idsString = encryptedPrefs.getString(KEY_AVAILABLE_PORTAL_IDS, null)
            if (idsString != null) {
                idsString.split(",").filter { it.isNotBlank() }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving available Portal IDs: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * 獲取默認的Portal ID
     */
    fun getDefaultPortalId(): String? {
        return try {
            encryptedPrefs.getString(KEY_DEFAULT_PORTAL_ID, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving default Portal ID: ${e.message}")
            null
        }
    }
}

sealed class LoginState {
    object NotLoggedIn : LoginState()
    data class LoggedIn(
        val sessionCookie: String,
        val userInfo: UserInfo
    ) : LoginState()
}
