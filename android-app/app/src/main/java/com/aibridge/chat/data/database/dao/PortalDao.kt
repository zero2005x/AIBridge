package com.aibridge.chat.data.database.dao

import androidx.room.*
import com.aibridge.chat.data.database.entities.PortalConfigEntity
import com.aibridge.chat.data.database.entities.PortalDetailEntity
import com.aibridge.chat.data.database.entities.PortalUsageHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PortalConfigDao {
    
    @Query("SELECT * FROM portal_configs WHERE isActive = 1 ORDER BY lastUsed DESC, name ASC")
    fun getAllActivePortalConfigs(): Flow<List<PortalConfigEntity>>
    
    @Query("SELECT * FROM portal_configs WHERE id = :portalId")
    suspend fun getPortalConfigById(portalId: String): PortalConfigEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPortalConfig(config: PortalConfigEntity)
    
    @Update
    suspend fun updatePortalConfig(config: PortalConfigEntity)
    
    @Delete
    suspend fun deletePortalConfig(config: PortalConfigEntity)
    
    @Query("UPDATE portal_configs SET lastUsed = :timestamp, usageCount = usageCount + 1 WHERE id = :portalId")
    suspend fun updateLastUsed(portalId: String, timestamp: Long)
    
    @Query("SELECT * FROM portal_configs ORDER BY usageCount DESC, lastUsed DESC LIMIT :limit")
    suspend fun getMostUsedPortals(limit: Int = 5): List<PortalConfigEntity>
}

@Dao
interface PortalDetailDao {
    
    @Query("SELECT * FROM portal_details WHERE isAccessible = 1 ORDER BY name ASC")
    fun getAllAccessiblePortalDetails(): Flow<List<PortalDetailEntity>>
    
    @Query("SELECT * FROM portal_details WHERE id = :portalId")
    suspend fun getPortalDetailById(portalId: String): PortalDetailEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPortalDetail(detail: PortalDetailEntity)
    
    @Update
    suspend fun updatePortalDetail(detail: PortalDetailEntity)
    
    @Delete
    suspend fun deletePortalDetail(detail: PortalDetailEntity)
    
    @Query("SELECT * FROM portal_details WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
    suspend fun searchPortals(query: String): List<PortalDetailEntity>
    
    @Query("SELECT * FROM portal_details WHERE category = :category ORDER BY name ASC")
    suspend fun getPortalsByCategory(category: String): List<PortalDetailEntity>
    
    @Query("UPDATE portal_details SET lastChecked = :timestamp WHERE id = :portalId")
    suspend fun updateLastChecked(portalId: String, timestamp: Long)
}

@Dao
interface PortalUsageHistoryDao {
    
    @Query("SELECT * FROM portal_usage_history WHERE portalId = :portalId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getPortalUsageHistory(portalId: String, limit: Int = 10): List<PortalUsageHistoryEntity>
    
    @Insert
    suspend fun insertUsageHistory(history: PortalUsageHistoryEntity)
    
    @Query("DELETE FROM portal_usage_history WHERE timestamp < :beforeTimestamp")
    suspend fun cleanupOldHistory(beforeTimestamp: Long)
    
    @Query("""
        SELECT portalId, COUNT(*) as totalUsage, 
               AVG(CASE WHEN success = 1 THEN 1.0 ELSE 0.0 END) as successRate,
               AVG(responseTime) as avgResponseTime,
               MAX(timestamp) as lastUsed
        FROM portal_usage_history 
        WHERE timestamp > :afterTimestamp 
        GROUP BY portalId
    """)
    suspend fun getPortalStats(afterTimestamp: Long): List<PortalStatsResult>
}

data class PortalStatsResult(
    val portalId: String,
    val totalUsage: Int,
    val successRate: Double,
    val avgResponseTime: Double,
    val lastUsed: Long
)
