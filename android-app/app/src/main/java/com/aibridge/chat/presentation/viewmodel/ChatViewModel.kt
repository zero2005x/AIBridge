package com.aibridge.chat.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aibridge.chat.data.repository.ChatRepository
import com.aibridge.chat.data.repository.AuthRepository
import com.aibridge.chat.data.repository.PortalRepository
import com.aibridge.chat.data.repository.LoginState
import com.aibridge.chat.domain.model.ChatSession
import com.aibridge.chat.domain.model.Message
import com.aibridge.chat.domain.model.PortalConfig
import com.aibridge.chat.utils.ChatDebugHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val portalRepository: PortalRepository,
    private val chatDebugHelper: ChatDebugHelper
) : ViewModel() {
    
    companion object {
        private const val TAG = "ChatViewModel"
    }
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    private val _currentSessionId = MutableStateFlow<String?>(null)
    
    // Portal相關的狀態
    private val _selectedPortalConfig = MutableStateFlow<PortalConfig?>(null)
    val selectedPortalConfig: StateFlow<PortalConfig?> = _selectedPortalConfig.asStateFlow()
    
    // 可用的Portal配置列表
    val availablePortalConfigs: StateFlow<List<PortalConfig>> = portalRepository.getAllPortalConfigs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val sessions: StateFlow<List<ChatSession>> = chatRepository.getAllSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val currentSessionMessages: StateFlow<List<Message>> = _currentSessionId
        .filterNotNull()
        .flatMapLatest { sessionId ->
            chatRepository.getSessionWithMessages(sessionId).map { it.second }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    init {
        Log.d(TAG, "ChatViewModel initialized")
        // 監聽會話列表變化，如果沒有當前會話且有可用會話，自動選擇第一個
        viewModelScope.launch {
            sessions.collect { sessionList ->
                Log.d(TAG, "Sessions updated: ${sessionList.size} sessions")
                if (_currentSessionId.value == null && sessionList.isNotEmpty()) {
                    Log.d(TAG, "Auto-selecting first session: ${sessionList.first().id}")
                    selectSession(sessionList.first().id)
                }
            }
        }
    }
    
    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.SelectSession -> selectSession(event.sessionId)
            is ChatEvent.CreateNewSession -> createNewSession(event.title, event.service, event.model)
            is ChatEvent.SendMessage -> sendMessage(event.content)
            is ChatEvent.DeleteSession -> deleteSession(event.sessionId)
            is ChatEvent.UpdateSessionTitle -> updateSessionTitle(event.sessionId, event.title)
            is ChatEvent.SelectPortalConfig -> selectPortalConfig(event.portalConfig)
            is ChatEvent.UpdatePortalParameters -> updatePortalParameters(event.portalConfig)
            ChatEvent.ClearError -> clearError()
            ChatEvent.RunDiagnostics -> runChatDiagnostics()
            ChatEvent.TestChatFlow -> testChatFlow()
        }
    }
    
    private fun selectSession(sessionId: String) {
        _currentSessionId.value = sessionId
        _uiState.value = _uiState.value.copy(selectedSessionId = sessionId)
    }
    
    private fun createNewSession(title: String, service: String, model: String) {
        viewModelScope.launch {
            try {
                val session = chatRepository.createSession(
                    title = title.ifBlank { "新對話" },
                    service = service,
                    model = model
                )
                selectSession(session.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "創建對話失敗: ${e.message}"
                )
            }
        }
    }
    
    private suspend fun ensureActiveSession(): String? {
        var sessionId = _currentSessionId.value
        if (sessionId == null) {
            Log.d(TAG, "No active session, creating default session")
            try {
                val session = chatRepository.createSession(
                    title = "新對話",
                    service = "openai",
                    model = "gpt-3.5-turbo"
                )
                sessionId = session.id
                selectSession(sessionId)
                Log.d(TAG, "Created default session: $sessionId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create default session: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "無法創建聊天會話: ${e.message}"
                )
                return null
            }
        }
        return sessionId
    }
    
    private fun sendMessage(content: String) {
        Log.d(TAG, "sendMessage called with content: $content")
        
        if (content.isBlank()) {
            Log.w(TAG, "Message content is blank, ignoring")
            return
        }
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // 確保有活動會話
                val sessionId = ensureActiveSession()
                if (sessionId == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@launch
                }
                
                Log.d(TAG, "Sending message to session: $sessionId")
                Log.d(TAG, "Sending message: $content")
                
                // 獲取當前登入狀態和憑證
                val loginState = authRepository.loginState.value
                if (loginState !is LoginState.LoggedIn) {
                    Log.w(TAG, "User not logged in")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "請先登入系統"
                    )
                    return@launch
                }
                
                // 從 SharedPreferences 獲取登入憑證
                val credentials = authRepository.getStoredCredentials()
                if (credentials == null) {
                    Log.e(TAG, "Could not retrieve stored credentials")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "無法獲取登入憑證"
                    )
                    return@launch
                }
                
                Log.d(TAG, "Using credentials: username=${credentials.username}, baseUrl=${credentials.baseUrl}")
                
                // 檢查是否有選擇的Portal配置
                val portalConfig = _selectedPortalConfig.value
                val messageToSend = if (portalConfig != null) {
                    // 使用Portal配置處理消息
                    Log.d(TAG, "Using Portal config: ${portalConfig.name} (ID: ${portalConfig.id})")
                    
                    // 如果Portal有USERPROMPT參數，使用它；否則使用原始消息
                    val userPromptParam = portalConfig.parameters["USERPROMPT"]
                    if (userPromptParam != null) {
                        // 將用戶輸入填入USERPROMPT參數
                        val updatedParam = userPromptParam.copy(value = content)
                        val updatedConfig = portalConfig.copy(
                            parameters = portalConfig.parameters.toMutableMap().apply {
                                put("USERPROMPT", updatedParam)
                            }
                        )
                        
                        // 更新Portal配置
                        _selectedPortalConfig.value = updatedConfig
                        _uiState.value = _uiState.value.copy(selectedPortalConfig = updatedConfig)
                        
                        // 記錄Portal使用
                        try {
                            portalRepository.recordPortalUsage(
                                portalId = portalConfig.id,
                                sessionId = sessionId,
                                parameters = updatedConfig.parameters.mapValues { it.value.value },
                                success = true,
                                responseTime = 0L
                            )
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to record portal usage: ${e.message}")
                        }
                        
                        content // 仍然使用原始內容發送，但Portal配置已更新
                    } else {
                        content
                    }
                } else {
                    Log.d(TAG, "No Portal config selected, using direct message sending")
                    content
                }
                
                // 調用 AI 服務
                val result = chatRepository.sendMessageToAI(
                    sessionId = sessionId,
                    message = messageToSend,
                    username = credentials.username,
                    password = credentials.password,
                    baseUrl = credentials.baseUrl,
                    portalConfig = _selectedPortalConfig.value
                )
                
                result.fold(
                    onSuccess = { _ ->
                        Log.i(TAG, "Message sent successfully, AI responded")
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    },
                    onFailure = { error ->
                        Log.w(TAG, "Failed to get AI response: ${error.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "AI 回應失敗: ${error.message}"
                        )
                    }
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "發送訊息失敗: ${e.message}"
                )
            }
        }
    }
    
    private fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            try {
                chatRepository.deleteSession(sessionId)
                if (_currentSessionId.value == sessionId) {
                    _currentSessionId.value = null
                    _uiState.value = _uiState.value.copy(selectedSessionId = null)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "刪除對話失敗: ${e.message}"
                )
            }
        }
    }
    
    private fun updateSessionTitle(sessionId: String, title: String) {
        viewModelScope.launch {
            try {
                val session = sessions.value.find { it.id == sessionId } ?: return@launch
                chatRepository.updateSession(session.copy(title = title))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "更新標題失敗: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun runChatDiagnostics() {
        viewModelScope.launch {
            Log.i(TAG, "Running chat functionality diagnostics")
            try {
                val result = chatDebugHelper.diagnoseChatIssue()
                Log.i(TAG, "Chat diagnostics completed:")
                Log.i(TAG, result.getSummary())
                
                if (!result.success) {
                    val errorMessage = "聊天診斷發現問題:\n${result.errors.joinToString("\n")}"
                    _uiState.value = _uiState.value.copy(errorMessage = errorMessage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Chat diagnostics failed: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "診斷失敗: ${e.message}"
                )
            }
        }
    }
    
    fun testChatFlow() {
        val sessionId = _currentSessionId.value
        if (sessionId == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "請先選擇或創建一個聊天會話"
            )
            return
        }
        
        viewModelScope.launch {
            Log.i(TAG, "Testing chat flow")
            try {
                val result = chatDebugHelper.testChatFlow(sessionId)
                Log.i(TAG, "Chat test result: ${result.message}")
                
                if (!result.success) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "聊天測試失敗: ${result.message}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Chat test failed: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "測試失敗: ${e.message}"
                )
            }
        }
    }
    
    // Portal相關方法
    private fun selectPortalConfig(portalConfig: PortalConfig) {
        _selectedPortalConfig.value = portalConfig
        _uiState.value = _uiState.value.copy(selectedPortalConfig = portalConfig)
        
        // 記錄Portal使用
        viewModelScope.launch {
            try {
                portalRepository.recordPortalUsage(
                    portalId = portalConfig.id,
                    sessionId = _currentSessionId.value ?: "",
                    parameters = portalConfig.parameters.mapValues { it.value.value },
                    success = true,
                    responseTime = 0L
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to record portal usage: ${e.message}")
            }
        }
        
        Log.d(TAG, "Selected Portal config: ${portalConfig.name} (ID: ${portalConfig.id})")
    }
    
    private fun updatePortalParameters(portalConfig: PortalConfig) {
        _selectedPortalConfig.value = portalConfig
        _uiState.value = _uiState.value.copy(selectedPortalConfig = portalConfig)
        
        // 保存更新的Portal配置
        viewModelScope.launch {
            try {
                portalRepository.savePortalConfig(portalConfig)
                Log.d(TAG, "Updated Portal config parameters: ${portalConfig.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save Portal config: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "保存Portal配置失敗: ${e.message}"
                )
            }
        }
    }
}

data class ChatUiState(
    val selectedSessionId: String? = null,
    val selectedPortalConfig: PortalConfig? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed class ChatEvent {
    data class SelectSession(val sessionId: String) : ChatEvent()
    data class CreateNewSession(val title: String, val service: String, val model: String) : ChatEvent()
    data class SendMessage(val content: String) : ChatEvent()
    data class DeleteSession(val sessionId: String) : ChatEvent()
    data class UpdateSessionTitle(val sessionId: String, val title: String) : ChatEvent()
    data class SelectPortalConfig(val portalConfig: PortalConfig) : ChatEvent()
    data class UpdatePortalParameters(val portalConfig: PortalConfig) : ChatEvent()
    object ClearError : ChatEvent()
    object RunDiagnostics : ChatEvent()
    object TestChatFlow : ChatEvent()
}
