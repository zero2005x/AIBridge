package com.aibridge.chat.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 安全管理器 - 負責加密存儲 API Keys 和敏感資料
 */
@Singleton
class SecurityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val KEYSTORE_ALIAS = "AIChatAppKey"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
        
        private const val PREFS_NAME = "secure_prefs"
        private const val KEY_API_KEYS = "api_keys"
        private const val KEY_PORTAL_CREDENTIALS = "portal_credentials"
    }
    
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    init {
        generateKeyIfNeeded()
    }
    
    /**
     * 生成 Android Keystore 中的密鑰（如果不存在）
     */
    private fun generateKeyIfNeeded() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        
        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .setRandomizedEncryptionRequired(false)
                .build()
            
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }
    
    /**
     * 加密文本
     */
    fun encrypt(plainText: String): String {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        
        val secretKey = keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(plainText.toByteArray())
        
        // 將 IV 和加密數據組合
        val combined = iv + encryptedData
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }
    
    /**
     * 解密文本
     */
    fun decrypt(encryptedText: String): String {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        
        val secretKey = keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
        val combined = Base64.decode(encryptedText, Base64.DEFAULT)
        
        // 分離 IV 和加密數據
        val iv = combined.sliceArray(0..GCM_IV_LENGTH - 1)
        val encryptedData = combined.sliceArray(GCM_IV_LENGTH until combined.size)
        
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        
        val decryptedData = cipher.doFinal(encryptedData)
        return String(decryptedData)
    }
    
    /**
     * 安全存儲 API Key
     */
    fun storeApiKey(service: String, apiKey: String) {
        val encryptedKey = encrypt(apiKey)
        encryptedPrefs.edit()
            .putString("${KEY_API_KEYS}_$service", encryptedKey)
            .apply()
    }
    
    /**
     * 獲取 API Key
     */
    fun getApiKey(service: String): String? {
        val encryptedKey = encryptedPrefs.getString("${KEY_API_KEYS}_$service", null)
        return encryptedKey?.let { decrypt(it) }
    }
    
    /**
     * 刪除 API Key
     */
    fun removeApiKey(service: String) {
        encryptedPrefs.edit()
            .remove("${KEY_API_KEYS}_$service")
            .apply()
    }
    
    /**
     * 安全存儲 Portal 憑證
     */
    fun storePortalCredentials(username: String, password: String, baseUrl: String) {
        val credentials = "$username|$password|$baseUrl"
        val encryptedCredentials = encrypt(credentials)
        encryptedPrefs.edit()
            .putString(KEY_PORTAL_CREDENTIALS, encryptedCredentials)
            .apply()
    }
    
    /**
     * 獲取 Portal 憑證
     */
    fun getPortalCredentials(): Triple<String, String, String>? {
        val encryptedCredentials = encryptedPrefs.getString(KEY_PORTAL_CREDENTIALS, null)
        return encryptedCredentials?.let {
            val decrypted = decrypt(it)
            val parts = decrypted.split("|")
            if (parts.size == 3) {
                Triple(parts[0], parts[1], parts[2])
            } else null
        }
    }
    
    /**
     * 清除所有憑證
     */
    fun clearAllCredentials() {
        encryptedPrefs.edit().clear().apply()
    }
    
    /**
     * 檢查是否有 Portal 憑證
     */
    fun hasPortalCredentials(): Boolean {
        return encryptedPrefs.contains(KEY_PORTAL_CREDENTIALS)
    }
    
    /**
     * 檢查是否有指定服務的 API Key
     */
    fun hasApiKey(service: String): Boolean {
        return encryptedPrefs.contains("${KEY_API_KEYS}_$service")
    }
    
    /**
     * 獲取所有已存儲的 API Key 服務列表
     */
    fun getStoredApiKeyServices(): List<String> {
        return encryptedPrefs.all.keys
            .filter { it.startsWith(KEY_API_KEYS) }
            .map { it.removePrefix("${KEY_API_KEYS}_") }
    }
}
