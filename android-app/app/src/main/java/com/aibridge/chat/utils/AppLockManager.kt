package com.aibridge.chat.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 應用鎖定管理器 - 處理應用自動鎖定功能
 */
@Singleton
class AppLockManager @Inject constructor() {
    
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()
    
    private var lockJob: Job? = null
    private var lockTimeoutMs: Long = 5 * 60 * 1000L // 預設5分鐘
    
    /**
     * 設定自動鎖定超時時間（毫秒）
     */
    fun setLockTimeout(timeoutMs: Long) {
        this.lockTimeoutMs = timeoutMs
    }
    
    /**
     * 開始計時器，超時後自動鎖定應用
     */
    fun startLockTimer(scope: CoroutineScope) {
        cancelLockTimer()
        
        if (lockTimeoutMs <= 0) return // 如果超時設為0，則不自動鎖定
        
        lockJob = scope.launch {
            delay(lockTimeoutMs)
            lockApp()
        }
    }
    
    /**
     * 取消鎖定計時器
     */
    fun cancelLockTimer() {
        lockJob?.cancel()
        lockJob = null
    }
    
    /**
     * 重置計時器（用戶有活動時調用）
     */
    fun resetLockTimer(scope: CoroutineScope) {
        if (_isLocked.value) return
        startLockTimer(scope)
    }
    
    /**
     * 手動鎖定應用
     */
    fun lockApp() {
        _isLocked.value = true
        cancelLockTimer()
    }
    
    /**
     * 解鎖應用
     */
    fun unlockApp() {
        _isLocked.value = false
    }
    
    /**
     * 應用進入前台時調用
     */
    fun onAppForegrounded(scope: CoroutineScope) {
        if (!_isLocked.value) {
            startLockTimer(scope)
        }
    }
    
    /**
     * 應用進入後台時調用
     */
    fun onAppBackgrounded() {
        cancelLockTimer()
    }
    
    /**
     * 用戶活動時調用（觸摸、按鍵等）
     */
    fun onUserActivity(scope: CoroutineScope) {
        if (!_isLocked.value) {
            resetLockTimer(scope)
        }
    }
}

/**
 * 擴展函數：為 LifecycleOwner 添加應用鎖定管理
 */
fun AppLockManager.observeWithLifecycle(lifecycleOwner: LifecycleOwner) {
    onAppForegrounded(lifecycleOwner.lifecycleScope)
}
