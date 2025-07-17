package com.aibridge.chat.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aibridge.chat.data.repository.PortalRepository
import com.aibridge.chat.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PortalManagementViewModel @Inject constructor(
    private val portalRepository: PortalRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "PortalManagementViewModel"
    }

    private val _uiState = MutableStateFlow(PortalManagementUiState())
    val uiState: StateFlow<PortalManagementUiState> = _uiState.asStateFlow()

    // Portal配置列表
    val portalConfigs = portalRepository.getAllPortalConfigs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        Log.d(TAG, "PortalManagementViewModel initialized")
        loadPortalConfigs()
        createDefaultConfigsIfNeeded()
    }

    fun handleEvent(event: PortalManagementEvent) {
        when (event) {
            is PortalManagementEvent.LoadPortalConfigs -> loadPortalConfigs()
            is PortalManagementEvent.SelectPortal -> selectPortal(event.portalId)
            is PortalManagementEvent.CreateNewPortal -> createNewPortal()
            is PortalManagementEvent.EditPortal -> editPortal(event.portalConfig)
            is PortalManagementEvent.SavePortal -> savePortal(event.portalConfig)
            is PortalManagementEvent.DeletePortal -> deletePortal(event.portalConfig)
            is PortalManagementEvent.UpdateParameter -> updateParameter(event.paramName, event.parameter)
            is PortalManagementEvent.AddParameter -> addParameter(event.parameter)
            is PortalManagementEvent.RemoveParameter -> removeParameter(event.paramName)
            is PortalManagementEvent.DiscoverPortals -> discoverPortals()
            is PortalManagementEvent.SearchPortals -> searchPortals(event.query)
            is PortalManagementEvent.LoadPortalDetail -> loadPortalDetail(event.portalId)
            is PortalManagementEvent.ClearSelection -> clearSelection()
            is PortalManagementEvent.SetActiveTab -> setActiveTab(event.tab)
        }
    }

    private fun loadPortalConfigs() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                // Portal configs are automatically loaded via Flow
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load portal configs: ${e.message}", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "載入Portal配置失敗：${e.message}"
                    )
                }
            }
        }
    }

    private fun selectPortal(portalId: String) {
        viewModelScope.launch {
            try {
                val portalConfig = portalRepository.getPortalConfigById(portalId)
                _uiState.update { 
                    it.copy(
                        selectedPortalConfig = portalConfig,
                        editingPortalConfig = portalConfig?.copy()
                    )
                }
                
                // 同時載入Portal詳細信息
                loadPortalDetail(portalId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to select portal: ${e.message}", e)
                _uiState.update { 
                    it.copy(errorMessage = "選擇Portal失敗：${e.message}")
                }
            }
        }
    }

    private fun createNewPortal() {
        val newPortal = PortalConfig(
            id = "",
            name = "新Portal",
            description = "",
            parameters = mapOf(
                "USERPROMPT" to PortalParameter(
                    name = "USERPROMPT",
                    value = "",
                    type = ParameterType.TEXTAREA,
                    isRequired = true,
                    description = "用戶輸入的提示文字",
                    placeholder = "請輸入您的問題..."
                )
            )
        )
        
        _uiState.update { 
            it.copy(
                editingPortalConfig = newPortal,
                isEditing = true
            )
        }
    }

    private fun editPortal(portalConfig: PortalConfig) {
        _uiState.update { 
            it.copy(
                editingPortalConfig = portalConfig.copy(),
                isEditing = true
            )
        }
    }

    private fun savePortal(portalConfig: PortalConfig) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                // 驗證Portal ID
                val finalConfig = if (portalConfig.id.isEmpty()) {
                    portalConfig.copy(id = generatePortalId())
                } else {
                    portalConfig
                }
                
                portalRepository.savePortalConfig(finalConfig)
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        isEditing = false,
                        editingPortalConfig = null,
                        selectedPortalConfig = finalConfig,
                        successMessage = "Portal配置已保存"
                    )
                }
                
                Log.d(TAG, "Portal saved successfully: ${finalConfig.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save portal: ${e.message}", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "保存Portal失敗：${e.message}"
                    )
                }
            }
        }
    }

    private fun deletePortal(portalConfig: PortalConfig) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                portalRepository.deletePortalConfig(portalConfig)
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        selectedPortalConfig = null,
                        editingPortalConfig = null,
                        successMessage = "Portal已刪除"
                    )
                }
                
                Log.d(TAG, "Portal deleted successfully: ${portalConfig.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete portal: ${e.message}", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "刪除Portal失敗：${e.message}"
                    )
                }
            }
        }
    }

    private fun updateParameter(paramName: String, parameter: PortalParameter) {
        _uiState.update { state ->
            state.editingPortalConfig?.let { config ->
                val updatedParameters = config.parameters.toMutableMap()
                updatedParameters[paramName] = parameter
                
                state.copy(
                    editingPortalConfig = config.copy(parameters = updatedParameters)
                )
            } ?: state
        }
    }

    private fun addParameter(parameter: PortalParameter) {
        _uiState.update { state ->
            state.editingPortalConfig?.let { config ->
                val updatedParameters = config.parameters.toMutableMap()
                updatedParameters[parameter.name] = parameter
                
                state.copy(
                    editingPortalConfig = config.copy(parameters = updatedParameters)
                )
            } ?: state
        }
    }

    private fun removeParameter(paramName: String) {
        _uiState.update { state ->
            state.editingPortalConfig?.let { config ->
                val updatedParameters = config.parameters.toMutableMap()
                updatedParameters.remove(paramName)
                
                state.copy(
                    editingPortalConfig = config.copy(parameters = updatedParameters)
                )
            } ?: state
        }
    }

    private fun discoverPortals() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isDiscovering = true) }
                
                val discoveredPortals = portalRepository.discoverPortals()
                
                _uiState.update { 
                    it.copy(
                        isDiscovering = false,
                        discoveredPortals = discoveredPortals,
                        successMessage = "發現了 ${discoveredPortals.size} 個Portal"
                    )
                }
                
                Log.d(TAG, "Portal discovery completed: ${discoveredPortals.size} portals found")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to discover portals: ${e.message}", e)
                _uiState.update { 
                    it.copy(
                        isDiscovering = false,
                        errorMessage = "Portal發現失敗：${e.message}"
                    )
                }
            }
        }
    }

    private fun searchPortals(query: String) {
        viewModelScope.launch {
            try {
                if (query.isEmpty()) {
                    _uiState.update { it.copy(searchResults = emptyList()) }
                    return@launch
                }
                
                val searchResults = portalRepository.searchPortals(query)
                _uiState.update { it.copy(searchResults = searchResults) }
                
                Log.d(TAG, "Portal search completed: ${searchResults.size} results for '$query'")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to search portals: ${e.message}", e)
                _uiState.update { 
                    it.copy(errorMessage = "搜索Portal失敗：${e.message}")
                }
            }
        }
    }

    private fun loadPortalDetail(portalId: String) {
        viewModelScope.launch {
            try {
                val portalDetail = portalRepository.getPortalDetailById(portalId)
                _uiState.update { it.copy(selectedPortalDetail = portalDetail) }
                
                Log.d(TAG, "Portal detail loaded for: $portalId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load portal detail: ${e.message}", e)
                _uiState.update { 
                    it.copy(errorMessage = "載入Portal詳情失敗：${e.message}")
                }
            }
        }
    }

    private fun clearSelection() {
        _uiState.update { 
            it.copy(
                selectedPortalConfig = null,
                selectedPortalDetail = null,
                editingPortalConfig = null,
                isEditing = false
            )
        }
    }

    private fun setActiveTab(tab: PortalTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    private fun createDefaultConfigsIfNeeded() {
        viewModelScope.launch {
            try {
                portalRepository.createDefaultPortalConfigs()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create default configs: ${e.message}", e)
            }
        }
    }

    private fun generatePortalId(): String {
        return System.currentTimeMillis().toString()
    }

    fun clearMessages() {
        _uiState.update { 
            it.copy(
                errorMessage = null,
                successMessage = null
            )
        }
    }
}

data class PortalManagementUiState(
    val isLoading: Boolean = false,
    val isDiscovering: Boolean = false,
    val isEditing: Boolean = false,
    val selectedPortalConfig: PortalConfig? = null,
    val selectedPortalDetail: PortalDetail? = null,
    val editingPortalConfig: PortalConfig? = null,
    val discoveredPortals: List<PortalDetail> = emptyList(),
    val searchResults: List<PortalDetail> = emptyList(),
    val activeTab: PortalTab = PortalTab.MY_PORTALS,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

enum class PortalTab {
    MY_PORTALS,
    DISCOVER,
    SEARCH
}

sealed class PortalManagementEvent {
    object LoadPortalConfigs : PortalManagementEvent()
    data class SelectPortal(val portalId: String) : PortalManagementEvent()
    object CreateNewPortal : PortalManagementEvent()
    data class EditPortal(val portalConfig: PortalConfig) : PortalManagementEvent()
    data class SavePortal(val portalConfig: PortalConfig) : PortalManagementEvent()
    data class DeletePortal(val portalConfig: PortalConfig) : PortalManagementEvent()
    data class UpdateParameter(val paramName: String, val parameter: PortalParameter) : PortalManagementEvent()
    data class AddParameter(val parameter: PortalParameter) : PortalManagementEvent()
    data class RemoveParameter(val paramName: String) : PortalManagementEvent()
    object DiscoverPortals : PortalManagementEvent()
    data class SearchPortals(val query: String) : PortalManagementEvent()
    data class LoadPortalDetail(val portalId: String) : PortalManagementEvent()
    object ClearSelection : PortalManagementEvent()
    data class SetActiveTab(val tab: PortalTab) : PortalManagementEvent()
}
