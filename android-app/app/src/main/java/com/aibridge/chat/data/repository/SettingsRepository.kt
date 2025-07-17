package com.aibridge.chat.data.repository

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val preferences: SharedPreferences
) {
    
    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_DEFAULT_SERVICE = "default_service"
        private const val KEY_DEFAULT_MODEL = "default_model"
        private const val KEY_AUTO_SAVE_CHATS = "auto_save_chats"
        private const val KEY_MESSAGE_FONT_SIZE = "message_font_size"
        private const val KEY_ENABLE_NOTIFICATIONS = "enable_notifications"
        private const val KEY_MAX_CHAT_HISTORY = "max_chat_history"
        
        // 新增設定
        private const val KEY_AUTO_SCROLL = "auto_scroll"
        private const val KEY_SHOW_TIMESTAMPS = "show_timestamps"
        private const val KEY_MARKDOWN_ENABLED = "markdown_enabled"
        private const val KEY_CODE_HIGHLIGHT = "code_highlight"
        private const val KEY_REQUEST_TIMEOUT = "request_timeout"
        private const val KEY_RETRY_COUNT = "retry_count"
        private const val KEY_AUTO_RETRY = "auto_retry"
        private const val KEY_BIOMETRIC_AUTH = "biometric_auth"
        private const val KEY_AUTO_BACKUP = "auto_backup"
        private const val KEY_BACKUP_FREQUENCY = "backup_frequency"
        
        // Default values
        private const val DEFAULT_THEME_MODE = "system" // system, light, dark
        private const val DEFAULT_LANGUAGE = "zh-TW"
        private const val DEFAULT_SERVICE = "openai"
        private const val DEFAULT_MODEL = "gpt-3.5-turbo"
        private const val DEFAULT_FONT_SIZE = 14
        private const val DEFAULT_MAX_HISTORY = 100
        private const val DEFAULT_REQUEST_TIMEOUT = 30 // seconds
        private const val DEFAULT_RETRY_COUNT = 3
        private const val DEFAULT_BACKUP_FREQUENCY = 7 // days
    }
    
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    
    private fun loadSettings(): AppSettings {
        return AppSettings(
            themeMode = preferences.getString(KEY_THEME_MODE, DEFAULT_THEME_MODE) ?: DEFAULT_THEME_MODE,
            language = preferences.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE,
            defaultService = preferences.getString(KEY_DEFAULT_SERVICE, DEFAULT_SERVICE) ?: DEFAULT_SERVICE,
            defaultModel = preferences.getString(KEY_DEFAULT_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL,
            autoSaveChats = preferences.getBoolean(KEY_AUTO_SAVE_CHATS, true),
            messageFontSize = preferences.getInt(KEY_MESSAGE_FONT_SIZE, DEFAULT_FONT_SIZE),
            enableNotifications = preferences.getBoolean(KEY_ENABLE_NOTIFICATIONS, true),
            maxChatHistory = preferences.getInt(KEY_MAX_CHAT_HISTORY, DEFAULT_MAX_HISTORY),
            // 新增設定
            autoScroll = preferences.getBoolean(KEY_AUTO_SCROLL, true),
            showTimestamps = preferences.getBoolean(KEY_SHOW_TIMESTAMPS, true),
            markdownEnabled = preferences.getBoolean(KEY_MARKDOWN_ENABLED, true),
            codeHighlight = preferences.getBoolean(KEY_CODE_HIGHLIGHT, true),
            requestTimeout = preferences.getInt(KEY_REQUEST_TIMEOUT, DEFAULT_REQUEST_TIMEOUT),
            retryCount = preferences.getInt(KEY_RETRY_COUNT, DEFAULT_RETRY_COUNT),
            autoRetry = preferences.getBoolean(KEY_AUTO_RETRY, true),
            biometricAuth = preferences.getBoolean(KEY_BIOMETRIC_AUTH, false),
            autoBackup = preferences.getBoolean(KEY_AUTO_BACKUP, false),
            backupFrequency = preferences.getInt(KEY_BACKUP_FREQUENCY, DEFAULT_BACKUP_FREQUENCY)
        )
    }
    
    fun updateThemeMode(themeMode: String) {
        preferences.edit().putString(KEY_THEME_MODE, themeMode).apply()
        _settings.value = _settings.value.copy(themeMode = themeMode)
    }
    
    fun updateLanguage(language: String) {
        preferences.edit().putString(KEY_LANGUAGE, language).apply()
        _settings.value = _settings.value.copy(language = language)
    }
    
    fun updateDefaultService(service: String) {
        preferences.edit().putString(KEY_DEFAULT_SERVICE, service).apply()
        _settings.value = _settings.value.copy(defaultService = service)
    }
    
    fun updateDefaultModel(model: String) {
        preferences.edit().putString(KEY_DEFAULT_MODEL, model).apply()
        _settings.value = _settings.value.copy(defaultModel = model)
    }
    
    fun updateAutoSaveChats(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_SAVE_CHATS, enabled).apply()
        _settings.value = _settings.value.copy(autoSaveChats = enabled)
    }
    
    fun updateMessageFontSize(fontSize: Int) {
        preferences.edit().putInt(KEY_MESSAGE_FONT_SIZE, fontSize).apply()
        _settings.value = _settings.value.copy(messageFontSize = fontSize)
    }
    
    fun updateEnableNotifications(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ENABLE_NOTIFICATIONS, enabled).apply()
        _settings.value = _settings.value.copy(enableNotifications = enabled)
    }
    
    fun updateMaxChatHistory(maxHistory: Int) {
        preferences.edit().putInt(KEY_MAX_CHAT_HISTORY, maxHistory).apply()
        _settings.value = _settings.value.copy(maxChatHistory = maxHistory)
    }
    
    // 新增的設定更新方法
    fun updateAutoScroll(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_SCROLL, enabled).apply()
        _settings.value = _settings.value.copy(autoScroll = enabled)
    }
    
    fun updateShowTimestamps(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SHOW_TIMESTAMPS, enabled).apply()
        _settings.value = _settings.value.copy(showTimestamps = enabled)
    }
    
    fun updateMarkdownEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_MARKDOWN_ENABLED, enabled).apply()
        _settings.value = _settings.value.copy(markdownEnabled = enabled)
    }
    
    fun updateCodeHighlight(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_CODE_HIGHLIGHT, enabled).apply()
        _settings.value = _settings.value.copy(codeHighlight = enabled)
    }
    
    fun updateRequestTimeout(timeout: Int) {
        preferences.edit().putInt(KEY_REQUEST_TIMEOUT, timeout).apply()
        _settings.value = _settings.value.copy(requestTimeout = timeout)
    }
    
    fun updateRetryCount(count: Int) {
        preferences.edit().putInt(KEY_RETRY_COUNT, count).apply()
        _settings.value = _settings.value.copy(retryCount = count)
    }
    
    fun updateAutoRetry(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_RETRY, enabled).apply()
        _settings.value = _settings.value.copy(autoRetry = enabled)
    }
    
    fun updateBiometricAuth(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_BIOMETRIC_AUTH, enabled).apply()
        _settings.value = _settings.value.copy(biometricAuth = enabled)
    }
    
    fun updateAutoBackup(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_BACKUP, enabled).apply()
        _settings.value = _settings.value.copy(autoBackup = enabled)
    }
    
    fun updateBackupFrequency(days: Int) {
        preferences.edit().putInt(KEY_BACKUP_FREQUENCY, days).apply()
        _settings.value = _settings.value.copy(backupFrequency = days)
    }
    
    fun resetToDefaults() {
        preferences.edit().clear().apply()
        _settings.value = loadSettings()
    }
}

data class AppSettings(
    val themeMode: String,
    val language: String,
    val defaultService: String,
    val defaultModel: String,
    val autoSaveChats: Boolean,
    val messageFontSize: Int,
    val enableNotifications: Boolean,
    val maxChatHistory: Int,
    // 新增設定
    val autoScroll: Boolean,
    val showTimestamps: Boolean,
    val markdownEnabled: Boolean,
    val codeHighlight: Boolean,
    val requestTimeout: Int,
    val retryCount: Int,
    val autoRetry: Boolean,
    val biometricAuth: Boolean,
    val autoBackup: Boolean,
    val backupFrequency: Int
)
