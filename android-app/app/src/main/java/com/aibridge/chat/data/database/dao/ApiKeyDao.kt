package com.aibridge.chat.data.database.dao

import androidx.room.*
import com.aibridge.chat.data.database.entities.ApiKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiKeyDao {
    
    @Query("SELECT * FROM api_keys WHERE isActive = 1 ORDER BY service ASC")
    fun getAllActiveApiKeys(): Flow<List<ApiKeyEntity>>
    
    @Query("SELECT * FROM api_keys WHERE service = :service AND isActive = 1 LIMIT 1")
    suspend fun getApiKeyByService(service: String): ApiKeyEntity?
    
    @Query("SELECT * FROM api_keys WHERE id = :keyId")
    suspend fun getApiKeyById(keyId: String): ApiKeyEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApiKey(apiKey: ApiKeyEntity)
    
    @Update
    suspend fun updateApiKey(apiKey: ApiKeyEntity)
    
    @Delete
    suspend fun deleteApiKey(apiKey: ApiKeyEntity)
    
    @Query("UPDATE api_keys SET encryptedKey = :encryptedKey, lastUsed = :timestamp WHERE id = :keyId")
    suspend fun updateApiKey(keyId: String, encryptedKey: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE api_keys SET lastUsed = :timestamp WHERE id = :keyId")
    suspend fun updateLastUsed(keyId: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE api_keys SET isActive = 0 WHERE id = :keyId")
    suspend fun deactivateApiKey(keyId: String)
    
    @Query("DELETE FROM api_keys WHERE isActive = 0")
    suspend fun deleteInactiveApiKeys()
    
    @Query("SELECT COUNT(*) FROM api_keys WHERE service = :service AND isActive = 1")
    suspend fun getApiKeyCountByService(service: String): Int
}
