package com.aibridge.chat.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkConfigManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "network_config"
        private const val KEY_CUSTOM_BASE_URL = "custom_base_url"
        private const val KEY_USE_CUSTOM_URL = "use_custom_url"
        private const val DEFAULT_BASE_URL = "https://dgb01p240102.japaneast.cloudapp.azure.com"
        
        // Alternative URLs for testing
        val FALLBACK_URLS = listOf(
            "https://dgb01p240102.japaneast.cloudapp.azure.com",
            // Add alternative URLs here if available
        )
    }
    
    private val preferences: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
                
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to regular SharedPreferences if encryption fails
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
    
    fun getBaseUrl(): String {
        return if (preferences.getBoolean(KEY_USE_CUSTOM_URL, false)) {
            preferences.getString(KEY_CUSTOM_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        } else {
            DEFAULT_BASE_URL
        }
    }
    
    fun setCustomBaseUrl(url: String) {
        preferences.edit()
            .putString(KEY_CUSTOM_BASE_URL, url)
            .putBoolean(KEY_USE_CUSTOM_URL, true)
            .apply()
    }
    
    fun useDefaultUrl() {
        preferences.edit()
            .putBoolean(KEY_USE_CUSTOM_URL, false)
            .apply()
    }
    
    fun getFallbackUrls(): List<String> = FALLBACK_URLS
    
    fun isUsingCustomUrl(): Boolean = preferences.getBoolean(KEY_USE_CUSTOM_URL, false)
}
