package com.aibridge.chat.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Portal配置模型
 */
@Parcelize
data class PortalConfig(
    val id: String,
    val name: String = "Portal $id",
    val description: String = "",
    val isActive: Boolean = true,
    val parameters: Map<String, PortalParameter> = emptyMap(),
    val lastUsed: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Portal參數模型
 */
@Parcelize
data class PortalParameter(
    val name: String,
    val value: String,
    val type: ParameterType = ParameterType.TEXT,
    val isRequired: Boolean = false,
    val description: String = "",
    val placeholder: String = ""
) : Parcelable

/**
 * 參數類型
 */
enum class ParameterType {
    TEXT,
    FILE,
    TEXTAREA,
    NUMBER,
    BOOLEAN,
    SELECT
}

/**
 * Portal詳細信息
 */
data class PortalDetail(
    val id: String,
    val name: String,
    val description: String,
    val category: String = "",
    val tags: List<String> = emptyList(),
    val parameters: List<ParameterDefinition> = emptyList(),
    val examples: List<PortalExample> = emptyList(),
    val isAccessible: Boolean = false,
    val lastUpdated: Long = 0L
)

/**
 * Portal參數定義
 */
data class ParameterDefinition(
    val name: String,
    val type: ParameterType,
    val isRequired: Boolean,
    val description: String,
    val defaultValue: String = "",
    val placeholder: String = "",
    val options: List<String> = emptyList(), // For SELECT type
    val minLength: Int? = null,
    val maxLength: Int? = null
)

/**
 * Portal使用示例
 */
data class PortalExample(
    val title: String,
    val description: String,
    val parameters: Map<String, String>
)

/**
 * Portal搜索結果
 */
data class PortalSearchResult(
    val portals: List<PortalDetail>,
    val totalCount: Int,
    val hasMore: Boolean
)

/**
 * Portal使用統計
 */
data class PortalUsageStats(
    val portalId: String,
    val totalUsage: Int,
    val successRate: Double,
    val averageResponseTime: Long,
    val lastUsed: Long
)
