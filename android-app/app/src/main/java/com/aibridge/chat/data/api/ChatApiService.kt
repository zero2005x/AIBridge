package com.aibridge.chat.data.api

import com.aibridge.chat.domain.model.AiResponse
import retrofit2.Response
import retrofit2.http.*

interface ChatApiService {
    
    @POST("/api/chat")
    @Headers("Content-Type: application/json")
    suspend fun sendMessage(
        @Header("Cookie") sessionCookie: String?,
        @Body request: ChatApiRequest
    ): Response<AiResponse>
    
    @GET("/api/models")
    suspend fun getAvailableModels(
        @Query("service") service: String
    ): Response<List<ModelInfo>>
    
    @GET("/api/services")
    suspend fun getAvailableServices(): Response<List<ServiceInfo>>
}

data class ChatApiRequest(
    val messages: List<ApiMessage>,
    val service: String,
    val model: String,
    val apiKey: String? = null,
    val systemPrompt: String? = null,
    val temperature: Float? = null,
    val maxTokens: Int? = null
)

data class ApiMessage(
    val role: String, // "user" or "assistant"
    val content: String
)

data class ModelInfo(
    val id: String,
    val name: String,
    val description: String? = null,
    val contextLength: Int? = null,
    val inputCost: Double? = null,
    val outputCost: Double? = null
)

data class ServiceInfo(
    val id: String,
    val name: String,
    val description: String? = null,
    val requiresApiKey: Boolean = true,
    val supportedModels: List<String> = emptyList()
)
