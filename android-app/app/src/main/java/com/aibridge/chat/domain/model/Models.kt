package com.aibridge.chat.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChatSession(
    val id: String,
    val title: String,
    val createdAt: Long,
    val lastModified: Long,
    val isActive: Boolean,
    val service: String,
    val model: String,
    val systemPrompt: String? = null,
    val messageCount: Int = 0,
    val totalTokens: Int = 0
) : Parcelable

@Parcelize
data class Message(
    val id: String,
    val sessionId: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long,
    val tokens: Int? = null,
    val model: String? = null,
    val service: String? = null,
    val attachments: List<Attachment>? = null
) : Parcelable

@Parcelize
data class Attachment(
    val id: String,
    val fileName: String,
    val fileType: String,
    val fileSize: Long,
    val filePath: String
) : Parcelable

@Parcelize
data class ApiKey(
    val id: String,
    val service: String,
    val displayName: String,
    val createdAt: Long,
    val lastUsed: Long? = null,
    val isActive: Boolean,
    val keyType: ApiKeyType,
    val hasKey: Boolean = true
) : Parcelable

enum class ApiKeyType {
    USER, PORTAL, SYSTEM
}

@Parcelize
data class PortalCredentials(
    val username: String,
    val password: String,
    val baseUrl: String
) : Parcelable

data class LoginResult(
    val success: Boolean,
    val message: String? = null,
    val sessionCookie: String? = null,
    val userInfo: UserInfo? = null
)

data class UserInfo(
    val username: String,
    val displayName: String? = null,
    val email: String? = null
)

data class AiResponse(
    val success: Boolean,
    val content: String? = null,
    val error: String? = null,
    val tokens: Int? = null,
    val model: String? = null,
    val service: String? = null
)

// 新的後端 API 回應格式 - 根據後端說明
data class BackendChatResponse(
    val reply: String? = null,
    val error: String? = null
)

data class ChatRequest(
    val messages: List<Message>,
    val service: String,
    val model: String,
    val apiKey: String? = null,
    val systemPrompt: String? = null,
    val temperature: Float? = null,
    val maxTokens: Int? = null
)

@Parcelize
data class QuickReply(
    val id: String,
    val text: String,
    val isActive: Boolean = true
) : Parcelable
