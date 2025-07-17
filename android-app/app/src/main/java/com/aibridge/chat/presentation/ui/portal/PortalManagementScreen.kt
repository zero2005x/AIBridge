package com.aibridge.chat.presentation.ui.portal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aibridge.chat.domain.model.PortalConfig
import com.aibridge.chat.presentation.viewmodel.PortalManagementViewModel
import com.aibridge.chat.presentation.viewmodel.PortalManagementEvent
import com.aibridge.chat.presentation.viewmodel.PortalTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortalManagementScreen(
    onBackClick: () -> Unit,
    viewModel: PortalManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val portalConfigs by viewModel.portalConfigs.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.handleEvent(PortalManagementEvent.LoadPortalConfigs)
    }

    // 顯示消息
    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) {
            // 可以在這裡顯示 SnackBar 或其他通知
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { 
                Text(
                    text = "Portal 管理",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            },
            actions = {
                // 發現Portal按鈕
                IconButton(
                    onClick = { viewModel.handleEvent(PortalManagementEvent.DiscoverPortals) }
                ) {
                    if (uiState.isDiscovering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Search, contentDescription = "發現Portal")
                    }
                }
                
                // 新增Portal按鈕
                IconButton(
                    onClick = { viewModel.handleEvent(PortalManagementEvent.CreateNewPortal) }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "新增Portal")
                }
            }
        )

        // Tab Row
        TabRow(
            selectedTabIndex = uiState.activeTab.ordinal
        ) {
            Tab(
                selected = uiState.activeTab == PortalTab.MY_PORTALS,
                onClick = { viewModel.handleEvent(PortalManagementEvent.SetActiveTab(PortalTab.MY_PORTALS)) },
                text = { Text("我的Portal") }
            )
            Tab(
                selected = uiState.activeTab == PortalTab.DISCOVER,
                onClick = { viewModel.handleEvent(PortalManagementEvent.SetActiveTab(PortalTab.DISCOVER)) },
                text = { Text("發現Portal") }
            )
            Tab(
                selected = uiState.activeTab == PortalTab.SEARCH,
                onClick = { viewModel.handleEvent(PortalManagementEvent.SetActiveTab(PortalTab.SEARCH)) },
                text = { Text("搜索Portal") }
            )
        }

        // Content
        when (uiState.activeTab) {
            PortalTab.MY_PORTALS -> {
                MyPortalsTab(
                    portalConfigs = portalConfigs,
                    selectedPortal = uiState.selectedPortalConfig,
                    onSelectPortal = { portalId ->
                        viewModel.handleEvent(PortalManagementEvent.SelectPortal(portalId))
                    },
                    onEditPortal = { portal ->
                        viewModel.handleEvent(PortalManagementEvent.EditPortal(portal))
                    },
                    onDeletePortal = { portal ->
                        viewModel.handleEvent(PortalManagementEvent.DeletePortal(portal))
                    },
                    isLoading = uiState.isLoading,
                    modifier = Modifier.weight(1f)
                )
            }
            PortalTab.DISCOVER -> {
                DiscoverPortalsTab(
                    discoveredPortals = uiState.discoveredPortals,
                    onDiscoverPortals = {
                        viewModel.handleEvent(PortalManagementEvent.DiscoverPortals)
                    },
                    onSelectPortal = { portalId ->
                        viewModel.handleEvent(PortalManagementEvent.LoadPortalDetail(portalId))
                    },
                    isDiscovering = uiState.isDiscovering,
                    modifier = Modifier.weight(1f)
                )
            }
            PortalTab.SEARCH -> {
                SearchPortalsTab(
                    searchResults = uiState.searchResults,
                    onSearch = { query ->
                        viewModel.handleEvent(PortalManagementEvent.SearchPortals(query))
                    },
                    onSelectPortal = { portalId ->
                        viewModel.handleEvent(PortalManagementEvent.LoadPortalDetail(portalId))
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Portal編輯對話框
    if (uiState.isEditing && uiState.editingPortalConfig != null) {
        val editingConfig = uiState.editingPortalConfig!!
        PortalEditDialog(
            portalConfig = editingConfig,
            onSave = { portal ->
                viewModel.handleEvent(PortalManagementEvent.SavePortal(portal))
            },
            onCancel = {
                viewModel.handleEvent(PortalManagementEvent.ClearSelection)
            },
            onUpdateParameter = { paramName, parameter ->
                viewModel.handleEvent(PortalManagementEvent.UpdateParameter(paramName, parameter))
            },
            onAddParameter = { parameter ->
                viewModel.handleEvent(PortalManagementEvent.AddParameter(parameter))
            },
            onRemoveParameter = { paramName ->
                viewModel.handleEvent(PortalManagementEvent.RemoveParameter(paramName))
            }
        )
    }

    // Portal詳情對話框
    if (uiState.selectedPortalDetail != null) {
        val selectedDetail = uiState.selectedPortalDetail!!
        PortalDetailDialog(
            portalDetail = selectedDetail,
            onDismiss = {
                viewModel.handleEvent(PortalManagementEvent.ClearSelection)
            },
            onCreateConfig = { portalDetail ->
                // 基於Portal詳情創建新配置
                val newConfig = PortalConfig(
                    id = portalDetail.id,
                    name = portalDetail.name,
                    description = portalDetail.description,
                    parameters = portalDetail.parameters.associate { paramDef ->
                        paramDef.name to com.aibridge.chat.domain.model.PortalParameter(
                            name = paramDef.name,
                            value = paramDef.defaultValue,
                            type = paramDef.type,
                            isRequired = paramDef.isRequired,
                            description = paramDef.description,
                            placeholder = paramDef.placeholder
                        )
                    }
                )
                viewModel.handleEvent(PortalManagementEvent.SavePortal(newConfig))
            }
        )
    }
}

@Composable
private fun MyPortalsTab(
    portalConfigs: List<PortalConfig>,
    selectedPortal: PortalConfig?,
    onSelectPortal: (String) -> Unit,
    onEditPortal: (PortalConfig) -> Unit,
    onDeletePortal: (PortalConfig) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (portalConfigs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "沒有Portal配置",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "點擊 + 按鈕來新增Portal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(portalConfigs) { portal ->
                    PortalConfigCard(
                        portalConfig = portal,
                        isSelected = selectedPortal?.id == portal.id,
                        onSelect = { onSelectPortal(portal.id) },
                        onEdit = { onEditPortal(portal) },
                        onDelete = { onDeletePortal(portal) }
                    )
                }
            }
        }
    }
}
