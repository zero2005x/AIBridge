package com.aibridge.chat.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aibridge.chat.data.repository.SettingsRepository
import com.aibridge.chat.data.repository.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    val settings: StateFlow<AppSettings> = settingsRepository.settings
    
    fun updateThemeMode(themeMode: String) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(themeMode)
        }
    }
    
    fun updateLanguage(language: String) {
        viewModelScope.launch {
            settingsRepository.updateLanguage(language)
        }
    }
    
    fun updateDefaultService(service: String) {
        viewModelScope.launch {
            settingsRepository.updateDefaultService(service)
        }
    }
    
    fun updateDefaultModel(model: String) {
        viewModelScope.launch {
            settingsRepository.updateDefaultModel(model)
        }
    }
    
    fun updateAutoSaveChats(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoSaveChats(enabled)
        }
    }
    
    fun updateMessageFontSize(fontSize: Int) {
        viewModelScope.launch {
            settingsRepository.updateMessageFontSize(fontSize)
        }
    }
    
    fun updateEnableNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateEnableNotifications(enabled)
        }
    }
    
    fun updateMaxChatHistory(maxHistory: Int) {
        viewModelScope.launch {
            settingsRepository.updateMaxChatHistory(maxHistory)
        }
    }
    
    // 新增的設定更新方法
    fun updateAutoScroll(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoScroll(enabled)
        }
    }
    
    fun updateShowTimestamps(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowTimestamps(enabled)
        }
    }
    
    fun updateMarkdownEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateMarkdownEnabled(enabled)
        }
    }
    
    fun updateCodeHighlight(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateCodeHighlight(enabled)
        }
    }
    
    fun updateRequestTimeout(timeout: Int) {
        viewModelScope.launch {
            settingsRepository.updateRequestTimeout(timeout)
        }
    }
    
    fun updateRetryCount(count: Int) {
        viewModelScope.launch {
            settingsRepository.updateRetryCount(count)
        }
    }
    
    fun updateAutoRetry(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoRetry(enabled)
        }
    }
    
    fun updateBiometricAuth(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateBiometricAuth(enabled)
        }
    }
    
    fun updateAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoBackup(enabled)
        }
    }
    
    fun updateBackupFrequency(days: Int) {
        viewModelScope.launch {
            settingsRepository.updateBackupFrequency(days)
        }
    }
    
    fun resetToDefaults() {
        viewModelScope.launch {
            settingsRepository.resetToDefaults()
        }
    }
}
