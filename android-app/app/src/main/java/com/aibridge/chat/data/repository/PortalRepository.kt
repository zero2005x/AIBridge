package com.aibridge.chat.data.repository

import android.util.Log
import com.aibridge.chat.data.database.dao.PortalConfigDao
import com.aibridge.chat.data.database.dao.PortalDetailDao
import com.aibridge.chat.data.database.dao.PortalUsageHistoryDao
import com.aibridge.chat.data.database.entities.PortalConfigEntity
import com.aibridge.chat.data.database.entities.PortalDetailEntity
import com.aibridge.chat.data.database.entities.PortalUsageHistoryEntity
import com.aibridge.chat.domain.model.*
import com.aibridge.chat.data.api.PortalDiscoveryService
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortalRepository @Inject constructor(
    private val portalConfigDao: PortalConfigDao,
    private val portalDetailDao: PortalDetailDao,
    private val portalUsageHistoryDao: PortalUsageHistoryDao,
    private val portalDiscoveryService: PortalDiscoveryService,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "PortalRepository"
    }

    /**
     * 獲取所有活動的Portal配置
     */
    fun getAllPortalConfigs(): Flow<List<PortalConfig>> {
        return portalConfigDao.getAllActivePortalConfigs().map { entities ->
            entities.map { it.toPortalConfig() }
        }
    }

    /**
     * 根據ID獲取Portal配置
     */
    suspend fun getPortalConfigById(portalId: String): PortalConfig? {
        return portalConfigDao.getPortalConfigById(portalId)?.toPortalConfig()
    }

    /**
     * 保存Portal配置
     */
    suspend fun savePortalConfig(config: PortalConfig) {
        try {
            portalConfigDao.insertPortalConfig(config.toEntity())
            Log.d(TAG, "Portal config saved: ${config.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save portal config: ${e.message}", e)
            throw e
        }
    }

    /**
     * 刪除Portal配置
     */
    suspend fun deletePortalConfig(config: PortalConfig) {
        try {
            portalConfigDao.deletePortalConfig(config.toEntity())
            Log.d(TAG, "Portal config deleted: ${config.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete portal config: ${e.message}", e)
            throw e
        }
    }

    /**
     * 獲取Portal詳細信息
     */
    suspend fun getPortalDetailById(portalId: String): PortalDetail? {
        return portalDetailDao.getPortalDetailById(portalId)?.toPortalDetail()
    }

    /**
     * 搜索Portal
     */
    suspend fun searchPortals(query: String): List<PortalDetail> {
        return try {
            portalDetailDao.searchPortals(query).map { it.toPortalDetail() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search portals: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * 發現並更新Portal信息
     */
    suspend fun discoverPortals(): List<PortalDetail> {
        return try {
            Log.d(TAG, "Starting portal discovery")
            val discoveredPortals = portalDiscoveryService.discoverPortals()
            
            // 保存發現的Portal詳細信息
            discoveredPortals.forEach { portal ->
                val entity = portal.toEntity()
                portalDetailDao.insertPortalDetail(entity)
            }
            
            Log.d(TAG, "Discovered ${discoveredPortals.size} portals")
            discoveredPortals
        } catch (e: Exception) {
            Log.e(TAG, "Failed to discover portals: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * 記錄Portal使用情況
     */
    suspend fun recordPortalUsage(
        portalId: String,
        sessionId: String,
        parameters: Map<String, String>,
        success: Boolean,
        responseTime: Long,
        errorMessage: String? = null
    ) {
        try {
            val historyEntity = PortalUsageHistoryEntity(
                id = UUID.randomUUID().toString(),
                portalId = portalId,
                sessionId = sessionId,
                parameters = gson.toJson(parameters),
                success = success,
                responseTime = responseTime,
                errorMessage = errorMessage
            )
            
            portalUsageHistoryDao.insertUsageHistory(historyEntity)
            portalConfigDao.updateLastUsed(portalId, System.currentTimeMillis())
            
            Log.d(TAG, "Portal usage recorded for: $portalId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record portal usage: ${e.message}", e)
        }
    }

    /**
     * 獲取最常用的Portal
     */
    suspend fun getMostUsedPortals(limit: Int = 5): List<PortalConfig> {
        return try {
            portalConfigDao.getMostUsedPortals(limit).map { it.toPortalConfig() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get most used portals: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * 創建默認Portal配置
     */
    suspend fun createDefaultPortalConfigs() {
        try {
            // 檢查是否已有配置
            val existingConfigs = portalConfigDao.getAllActivePortalConfigs()
            
            // 創建一些默認配置
            val defaultConfigs = listOf(
                PortalConfig(
                    id = "1",
                    name = "通用對話Portal",
                    description = "用於一般對話和問答",
                    parameters = mapOf(
                        "USERPROMPT" to PortalParameter(
                            name = "USERPROMPT",
                            value = "",
                            type = ParameterType.TEXTAREA,
                            isRequired = true,
                            description = "用戶輸入的提示文字",
                            placeholder = "請輸入您的問題或要求..."
                        ),
                        "USERUPLOADFILE" to PortalParameter(
                            name = "USERUPLOADFILE",
                            value = "data:application/octet-stream;base64,",
                            type = ParameterType.FILE,
                            isRequired = false,
                            description = "上傳的文件內容（Base64編碼）",
                            placeholder = "選擇要上傳的文件..."
                        )
                    )
                ),
                PortalConfig(
                    id = "13",
                    name = "自定義Portal",
                    description = "可自定義參數的Portal",
                    parameters = mapOf(
                        "USERPROMPT" to PortalParameter(
                            name = "USERPROMPT",
                            value = "",
                            type = ParameterType.TEXTAREA,
                            isRequired = true,
                            description = "用戶輸入的提示文字"
                        ),
                        "USERUPLOADFILE" to PortalParameter(
                            name = "USERUPLOADFILE",
                            value = "data:application/octet-stream;base64,",
                            type = ParameterType.FILE,
                            isRequired = false,
                            description = "上傳的文件內容"
                        )
                    )
                )
            )
            
            defaultConfigs.forEach { config ->
                if (portalConfigDao.getPortalConfigById(config.id) == null) {
                    portalConfigDao.insertPortalConfig(config.toEntity())
                }
            }
            
            Log.d(TAG, "Default portal configs created")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create default portal configs: ${e.message}", e)
        }
    }

    // 擴展函數用於實體轉換
    private fun PortalConfigEntity.toPortalConfig(): PortalConfig {
        val parametersMap = try {
            val type = object : com.google.gson.reflect.TypeToken<Map<String, PortalParameter>>() {}.type
            gson.fromJson<Map<String, PortalParameter>>(parameters, type)
                ?: emptyMap<String, PortalParameter>()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse parameters for portal $id: ${e.message}")
            emptyMap<String, PortalParameter>()
        }
        
        return PortalConfig(
            id = id,
            name = name,
            description = description,
            isActive = isActive,
            parameters = parametersMap,
            lastUsed = lastUsed,
            createdAt = createdAt
        )
    }

    private fun PortalConfig.toEntity(): PortalConfigEntity {
        return PortalConfigEntity(
            id = id,
            name = name,
            description = description,
            isActive = isActive,
            parameters = gson.toJson(parameters),
            lastUsed = lastUsed,
            createdAt = createdAt
        )
    }

    private fun PortalDetailEntity.toPortalDetail(): PortalDetail {
        val tagsList = try {
            val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(tags, type) ?: emptyList<String>()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse tags for portal $id: ${e.message}")
            emptyList<String>()
        }
        
        val parametersList = try {
            val type = object : com.google.gson.reflect.TypeToken<List<ParameterDefinition>>() {}.type
            gson.fromJson<List<ParameterDefinition>>(parameters, type) ?: emptyList<ParameterDefinition>()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse parameters for portal $id: ${e.message}")
            emptyList<ParameterDefinition>()
        }
        
        val examplesList = try {
            val type = object : com.google.gson.reflect.TypeToken<List<PortalExample>>() {}.type
            gson.fromJson<List<PortalExample>>(examples, type) ?: emptyList<PortalExample>()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse examples for portal $id: ${e.message}")
            emptyList<PortalExample>()
        }
        
        return PortalDetail(
            id = id,
            name = name,
            description = description,
            category = category,
            tags = tagsList,
            parameters = parametersList,
            examples = examplesList,
            isAccessible = isAccessible,
            lastUpdated = lastUpdated
        )
    }

    private fun PortalDetail.toEntity(): PortalDetailEntity {
        return PortalDetailEntity(
            id = id,
            name = name,
            description = description,
            category = category,
            tags = gson.toJson(tags),
            parameters = gson.toJson(parameters),
            examples = gson.toJson(examples),
            isAccessible = isAccessible,
            lastUpdated = lastUpdated
        )
    }
}
