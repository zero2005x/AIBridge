package com.aibridge.chat.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aibridge.chat.data.repository.AuthRepository
import com.aibridge.chat.data.repository.LoginState
import com.aibridge.chat.domain.model.PortalCredentials
import com.aibridge.chat.utils.NetworkUtils
import com.aibridge.chat.utils.NetworkDiagnostics
import com.aibridge.chat.utils.ServerConnectionTester
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val networkUtils: NetworkUtils,
    private val networkDiagnostics: NetworkDiagnostics,
    private val serverConnectionTester: ServerConnectionTester
) : ViewModel() {
    
    companion object {
        private const val TAG = "AuthViewModel"
    }
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    val loginStatus: StateFlow<LoginStatus> = authRepository.loginState.map { state ->
        Log.d(TAG, "Login state changed: $state")
        when (state) {
            is LoginState.NotLoggedIn -> LoginStatus.NotLoggedIn
            is LoginState.LoggedIn -> LoginStatus.LoggedIn
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LoginStatus.NotLoggedIn
    )
    
    fun login(username: String, password: String, baseUrl: String) {
        if (_uiState.value.isLoading) return
        
        Log.d(TAG, "Starting login process for user: $username, baseUrl: $baseUrl")
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                // Check network connectivity first
                if (!networkUtils.isNetworkAvailable()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "網路連線失敗：請檢查您的網路連線狀態"
                    )
                    return@launch
                }

                // Validate base URL format
                val validatedBaseUrl = if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
                    "https://$baseUrl"
                } else {
                    baseUrl
                }.trimEnd('/')

                // Check if we can resolve the hostname
                if (!networkUtils.canResolveHost(validatedBaseUrl)) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "無法解析伺服器位址：請檢查網址是否正確或網路設定"
                    )
                    return@launch
                }

                val credentials = PortalCredentials(
                    username = username.trim(),
                    password = password,
                    baseUrl = validatedBaseUrl
                )
                
                Log.d(TAG, "Calling authRepository.login with credentials")
                val result = authRepository.login(credentials)
                
                Log.d(TAG, "Login result: success=${result.success}, message=${result.message}")
                
                if (result.success) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = null
                    )
                    Log.i(TAG, "Login successful")
                } else {
                    val errorMsg = result.message ?: "登入失敗"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = errorMsg
                    )
                    Log.w(TAG, "Login failed: $errorMsg")
                }
            } catch (e: Exception) {
                val errorMsg = networkUtils.getNetworkErrorMessage(e.message ?: "未知錯誤")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorMsg
                )
                Log.e(TAG, "Login exception: $errorMsg", e)
            }
        }
    }
    
    fun runNetworkDiagnostics(targetUrl: String = "https://dgb01p240102.japaneast.cloudapp.azure.com") {
        viewModelScope.launch {
            Log.i(TAG, "Running network diagnostics for: $targetUrl")
            try {
                val result = networkDiagnostics.runFullDiagnostics(targetUrl)
                Log.i(TAG, "Network diagnostics completed:")
                Log.i(TAG, result.getSummary())
                
                if (!result.success) {
                    val errorMessage = "網路診斷失敗:\n${result.errors.joinToString("\n")}"
                    _uiState.value = _uiState.value.copy(errorMessage = errorMessage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network diagnostics failed: ${e.message}", e)
            }
        }
    }
    
    fun runServerConnectionTest(
        baseUrl: String = "https://dgb01p240102.japaneast.cloudapp.azure.com",
        testUsername: String? = null,
        testPassword: String? = null
    ) {
        viewModelScope.launch {
            Log.i(TAG, "Running server connection test for: $baseUrl")
            try {
                val result = serverConnectionTester.runFullConnectionTest(baseUrl, testUsername, testPassword)
                val summary = serverConnectionTester.getTestSummary(result)
                
                Log.i(TAG, "Server connection test completed:")
                Log.i(TAG, summary)
                
                if (!result.success) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "伺服器連線測試失敗:\n${result.message}"
                    )
                } else {
                    Log.i(TAG, "All server connection tests passed!")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server connection test failed: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "連線測試過程發生錯誤: ${e.message}"
                )
            }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun checkAccess() {
        viewModelScope.launch {
            authRepository.checkAccess()
        }
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

enum class LoginStatus {
    NotLoggedIn,
    LoggedIn
}
