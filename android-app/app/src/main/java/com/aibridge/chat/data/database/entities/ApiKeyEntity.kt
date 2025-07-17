package com.aibridge.chat.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_keys")
data class ApiKeyEntity(
    @PrimaryKey val id: String,
    val service: String,
    val encryptedKey: String,
    val displayName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long? = null,
    val isActive: Boolean = true,
    val keyType: String = "USER", // USER, PORTAL, SYSTEM
    val metadata: String? = null // JSON string for additional metadata
)
