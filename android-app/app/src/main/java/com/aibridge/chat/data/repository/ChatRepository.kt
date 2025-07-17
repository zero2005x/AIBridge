package com.aibridge.chat.data.repository

import android.util.Log
import com.aibridge.chat.data.api.AiChatApiService
import com.aibridge.chat.data.database.dao.ChatSessionDao
import com.aibridge.chat.data.database.dao.MessageDao
import com.aibridge.chat.data.database.entities.ChatSessionEntity
import com.aibridge.chat.data.database.entities.MessageEntity
import com.aibridge.chat.domain.model.ChatSession
import com.aibridge.chat.domain.model.Message
import com.aibridge.chat.domain.model.Attachment
import com.aibridge.chat.domain.model.PortalConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Singleton
class ChatRepository @Inject constructor(
    private val chatSessionDao: ChatSessionDao,
    private val messageDao: MessageDao,
    private val aiChatApiService: AiChatApiService,
    private val gson: Gson
) {
    
    companion object {
        private const val TAG = "ChatRepository"
    }
    
    fun getAllSessions(): Flow<List<ChatSession>> {
        return chatSessionDao.getAllActiveSessions().map { entities ->
            entities.map { entity ->
                entity.toDomainModel()
            }
        }
    }
    
    fun getSessionWithMessages(sessionId: String): Flow<Pair<ChatSession?, List<Message>>> {
        return combine(
            chatSessionDao.getAllActiveSessions(),
            messageDao.getMessagesBySession(sessionId)
        ) { sessions, messages ->
            val session = sessions.find { it.id == sessionId }?.toDomainModel()
            val messageList = messages.map { it.toDomainModel() }
            Pair(session, messageList)
        }
    }
    
    suspend fun createSession(
        title: String,
        service: String,
        model: String,
        systemPrompt: String? = null
    ): ChatSession {
        val sessionId = UUID.randomUUID().toString()
        val entity = ChatSessionEntity(
            id = sessionId,
            title = title,
            service = service,
            model = model,
            systemPrompt = systemPrompt
        )
        
        chatSessionDao.insertSession(entity)
        return entity.toDomainModel()
    }
    
    suspend fun updateSession(session: ChatSession) {
        val entity = ChatSessionEntity(
            id = session.id,
            title = session.title,
            createdAt = session.createdAt,
            lastModified = System.currentTimeMillis(),
            isActive = session.isActive,
            service = session.service,
            model = session.model,
            systemPrompt = session.systemPrompt
        )
        chatSessionDao.updateSession(entity)
    }
    
    suspend fun deleteSession(sessionId: String) {
        chatSessionDao.deactivateSession(sessionId)
        messageDao.deleteMessagesBySession(sessionId)
    }
    
    suspend fun addMessage(
        sessionId: String,
        content: String,
        isUser: Boolean,
        tokens: Int? = null,
        model: String? = null,
        service: String? = null,
        attachments: List<Attachment>? = null
    ): Message {
        val messageId = UUID.randomUUID().toString()
        val entity = MessageEntity(
            id = messageId,
            sessionId = sessionId,
            content = content,
            isUser = isUser,
            tokens = tokens,
            model = model,
            service = service,
            attachments = attachments?.let { gson.toJson(it) }
        )
        
        messageDao.insertMessage(entity)
        chatSessionDao.updateLastModified(sessionId, System.currentTimeMillis())
        
        return entity.toDomainModel()
    }
    
    suspend fun updateMessage(message: Message) {
        val entity = MessageEntity(
            id = message.id,
            sessionId = message.sessionId,
            content = message.content,
            isUser = message.isUser,
            timestamp = message.timestamp,
            tokens = message.tokens,
            model = message.model,
            service = message.service,
            attachments = message.attachments?.let { gson.toJson(it) }
        )
        messageDao.updateMessage(entity)
    }
    
    suspend fun deleteMessage(messageId: String) {
        val message = messageDao.getMessageById(messageId)
        message?.let { messageDao.deleteMessage(it) }
    }
    
    suspend fun getSessionCount(): Int {
        return chatSessionDao.getActiveSessionCount()
    }
    
    suspend fun searchMessages(query: String): Flow<List<Message>> {
        return messageDao.searchMessages(query).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    // Extension functions for converting between entities and domain models
    private fun ChatSessionEntity.toDomainModel(): ChatSession {
        return ChatSession(
            id = id,
            title = title,
            createdAt = createdAt,
            lastModified = lastModified,
            isActive = isActive,
            service = service,
            model = model,
            systemPrompt = systemPrompt
        )
    }
    
    private fun MessageEntity.toDomainModel(): Message {
        val attachmentList = attachments?.let {
            val type = object : TypeToken<List<Attachment>>() {}.type
            gson.fromJson<List<Attachment>>(it, type)
        }
        
        return Message(
            id = id,
            sessionId = sessionId,
            content = content,
            isUser = isUser,
            timestamp = timestamp,
            tokens = tokens,
            model = model,
            service = service,
            attachments = attachmentList
        )
    }
    
    /**
     * 發送訊息到 AI 並取得回應
     */
    suspend fun sendMessageToAI(
        sessionId: String,
        message: String,
        username: String,
        password: String,
        baseUrl: String,
        service: String = "openai",
        model: String = "gpt-3.5-turbo",
        portalConfig: PortalConfig? = null
    ): Result<Message> {
        return try {
            Log.d(TAG, "Sending message to AI for session: $sessionId")
            
            // 先添加用戶訊息
            val userMessage = addMessage(
                sessionId = sessionId,
                content = message,
                isUser = true
            )
            
            Log.d(TAG, "User message added, calling AI service")
            
            // 調用 AI 服務 - 根據後端說明，只需要這些參數
            val chatResponse = aiChatApiService.sendChatMessage(
                username = username,
                password = password,
                baseUrl = baseUrl,
                message = message,
                portalConfig = portalConfig
                // service 和 model 參數由後端內部處理，不需要從 Android 傳送
            )
            
            if (chatResponse.success) {
                Log.i(TAG, "AI response received successfully")
                
                // 添加 AI 回應訊息
                val aiMessage = addMessage(
                    sessionId = sessionId,
                    content = chatResponse.message,
                    isUser = false,
                    model = model,
                    service = service,
                    tokens = null  // BackendChatResponse 不包含 tokens 信息
                )
                
                Result.success(aiMessage)
            } else {
                Log.w(TAG, "AI response failed: ${chatResponse.message}")
                
                // 添加錯誤回應訊息
                val errorMessage = addMessage(
                    sessionId = sessionId,
                    content = "抱歉，AI 服務暫時無法回應：${chatResponse.message}",
                    isUser = false,
                    service = "system"
                )
                
                Result.failure(Exception(chatResponse.message))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message to AI: ${e.message}", e)
            
            try {
                // 添加錯誤回應訊息
                addMessage(
                    sessionId = sessionId,
                    content = "抱歉，發生錯誤：${e.message}",
                    isUser = false,
                    service = "system"
                )
            } catch (dbError: Exception) {
                Log.e(TAG, "Failed to save error message: ${dbError.message}")
            }
            
            Result.failure(e)
        }
    }
}
