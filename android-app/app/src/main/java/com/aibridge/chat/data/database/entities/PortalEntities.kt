package com.aibridge.chat.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "portal_configs")
data class PortalConfigEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val isActive: Boolean = true,
    val parameters: String, // JSON string for Map<String, PortalParameter>
    val lastUsed: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val usageCount: Int = 0
)

@Entity(tableName = "portal_details")
data class PortalDetailEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val category: String,
    val tags: String, // JSON string for List<String>
    val parameters: String, // JSON string for List<ParameterDefinition>
    val examples: String, // JSON string for List<PortalExample>
    val isAccessible: Boolean,
    val lastUpdated: Long,
    val lastChecked: Long = System.currentTimeMillis()
)

@Entity(tableName = "portal_usage_history")
data class PortalUsageHistoryEntity(
    @PrimaryKey val id: String,
    val portalId: String,
    val sessionId: String,
    val parameters: String, // JSON string for Map<String, String>
    val success: Boolean,
    val responseTime: Long,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
