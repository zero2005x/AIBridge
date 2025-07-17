package com.aibridge.chat.data.repository

import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import com.aibridge.chat.data.database.dao.ApiKeyDao
import com.aibridge.chat.data.database.entities.ApiKeyEntity
import com.aibridge.chat.domain.model.ApiKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyRepository @Inject constructor(
    private val apiKeyDao: ApiKeyDao,
    private val encryptedPrefs: SharedPreferences
) {
    
    companion object {
        private const val TAG = "ApiKeyRepository"
        private const val ENCRYPTION_KEY = "api_key_encryption_key"
        private const val ALGORITHM = "AES"
    }
    
    private val encryptionKey: SecretKey by lazy {
        getOrCreateEncryptionKey()
    }
    
    fun getAllApiKeys(): Flow<List<ApiKey>> {
        return apiKeyDao.getAllActiveApiKeys().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    suspend fun getApiKey(service: String): ApiKey? {
        return apiKeyDao.getApiKeyByService(service)?.toDomainModel()
    }
    
    suspend fun saveApiKey(service: String, displayName: String, plainApiKey: String): Result<Unit> {
        return try {
            val encryptedKey = encryptApiKey(plainApiKey)
            val entity = ApiKeyEntity(
                id = UUID.randomUUID().toString(),
                service = service,
                encryptedKey = encryptedKey,
                displayName = displayName,
                createdAt = System.currentTimeMillis(),
                isActive = true,
                keyType = "USER"
            )
            
            apiKeyDao.insertApiKey(entity)
            Log.d(TAG, "API key saved for service: $service")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save API key for $service: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getDecryptedApiKey(service: String): String? {
        return try {
            val entity = apiKeyDao.getApiKeyByService(service)
            entity?.let { decryptApiKey(it.encryptedKey) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt API key for $service: ${e.message}", e)
            null
        }
    }
    
    suspend fun deleteApiKey(keyId: String) {
        try {
            apiKeyDao.deactivateApiKey(keyId)
            Log.d(TAG, "API key deactivated: $keyId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete API key: ${e.message}", e)
        }
    }
    
    suspend fun updateLastUsed(keyId: String) {
        try {
            apiKeyDao.updateLastUsed(keyId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update last used timestamp: ${e.message}", e)
        }
    }
    
    private fun getOrCreateEncryptionKey(): SecretKey {
        val keyBase64 = encryptedPrefs.getString(ENCRYPTION_KEY, null)
        
        return if (keyBase64 != null) {
            try {
                val keyBytes = Base64.decode(keyBase64, Base64.DEFAULT)
                SecretKeySpec(keyBytes, ALGORITHM)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to decode stored key, creating new one")
                createNewEncryptionKey()
            }
        } else {
            createNewEncryptionKey()
        }
    }
    
    private fun createNewEncryptionKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM)
        keyGenerator.init(256)
        val secretKey = keyGenerator.generateKey()
        
        // Store the key
        val keyBase64 = Base64.encodeToString(secretKey.encoded, Base64.DEFAULT)
        encryptedPrefs.edit().putString(ENCRYPTION_KEY, keyBase64).apply()
        
        Log.d(TAG, "Created new encryption key")
        return secretKey
    }
    
    private fun encryptApiKey(plainText: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey)
        val encryptedBytes = cipher.doFinal(plainText.toByteArray())
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
    }
    
    private fun decryptApiKey(encryptedText: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey)
        val encryptedBytes = Base64.decode(encryptedText, Base64.DEFAULT)
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes)
    }
    
    private fun ApiKeyEntity.toDomainModel(): ApiKey {
        return ApiKey(
            id = id,
            service = service,
            displayName = displayName,
            createdAt = createdAt,
            hasKey = encryptedKey.isNotEmpty(),
            isActive = true,
            keyType = com.aibridge.chat.domain.model.ApiKeyType.USER
        )
    }
}
