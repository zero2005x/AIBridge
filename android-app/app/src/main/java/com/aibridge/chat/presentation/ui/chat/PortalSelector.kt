package com.aibridge.chat.presentation.ui.chat

import androidx.compose.animation.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aibridge.chat.domain.model.PortalConfig
import com.aibridge.chat.domain.model.PortalParameter
import com.aibridge.chat.presentation.viewmodel.PortalManagementViewModel
import com.aibridge.chat.presentation.viewmodel.PortalManagementEvent
import com.aibridge.chat.presentation.ui.portal.PortalEditDialog

/**
 * Portal選擇器組件，用於在聊天界面中選擇和配置Portal
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortalSelector(
    selectedPortalConfig: PortalConfig?,
    onPortalSelected: (PortalConfig) -> Unit,
    onPortalParametersChanged: (PortalConfig) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PortalManagementViewModel = hiltViewModel()
) {
    var showPortalSelector by remember { mutableStateOf(false) }
    var showParameterEditor by remember { mutableStateOf(false) }
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val portalConfigs by viewModel.portalConfigs.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.handleEvent(PortalManagementEvent.LoadPortalConfigs)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Portal選擇按鈕
        OutlinedButton(
            onClick = { showPortalSelector = true },
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = selectedPortalConfig?.name ?: "選擇Portal",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (selectedPortalConfig != null) {
                    Text(
                        text = "ID: ${selectedPortalConfig.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }

        // 參數編輯按鈕
        AnimatedVisibility(
            visible = selectedPortalConfig != null,
            enter = slideInHorizontally() + fadeIn(),
            exit = slideOutHorizontally() + fadeOut()
        ) {
            OutlinedIconButton(
                onClick = { showParameterEditor = true }
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "編輯參數"
                )
            }
        }

        // Portal管理按鈕
        OutlinedIconButton(
            onClick = { 
                viewModel.handleEvent(PortalManagementEvent.SetActiveTab(
                    com.aibridge.chat.presentation.viewmodel.PortalTab.MY_PORTALS
                ))
                // 這裡可以導航到Portal管理頁面，或者顯示Portal管理對話框
            }
        ) {
            Icon(
                Icons.Default.ManageAccounts,
                contentDescription = "管理Portal"
            )
        }
    }

    // Portal選擇對話框
    if (showPortalSelector) {
        PortalSelectorDialog(
            portalConfigs = portalConfigs,
            selectedPortalConfig = selectedPortalConfig,
            onPortalSelected = { portal ->
                onPortalSelected(portal)
                showPortalSelector = false
            },
            onDismiss = { showPortalSelector = false },
            onCreateNew = {
                viewModel.handleEvent(PortalManagementEvent.CreateNewPortal)
                showPortalSelector = false
            }
        )
    }

    // 參數編輯對話框
    if (showParameterEditor && selectedPortalConfig != null) {
        PortalParameterEditDialog(
            portalConfig = selectedPortalConfig,
            onSave = { updatedConfig ->
                onPortalParametersChanged(updatedConfig)
                showParameterEditor = false
            },
            onCancel = { showParameterEditor = false }
        )
    }
}

/**
 * Portal選擇對話框
 */
@Composable
private fun PortalSelectorDialog(
    portalConfigs: List<PortalConfig>,
    selectedPortalConfig: PortalConfig?,
    onPortalSelected: (PortalConfig) -> Unit,
    onDismiss: () -> Unit,
    onCreateNew: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.7f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "選擇Portal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 新增Portal按鈕
                        FilledTonalButton(
                            onClick = onCreateNew
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("新增")
                        }
                        
                        // 關閉按鈕
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "關閉")
                        }
                    }
                }

                Divider()

                // Portal列表
                if (portalConfigs.isEmpty()) {
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
                                "點擊新增按鈕來創建Portal",
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
                            PortalSelectionCard(
                                portalConfig = portal,
                                isSelected = selectedPortalConfig?.id == portal.id,
                                onSelect = { onPortalSelected(portal) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Portal選擇卡片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortalSelectionCard(
    portalConfig: PortalConfig,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                2.dp, 
                MaterialTheme.colorScheme.primary
            )
        } else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = portalConfig.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    
                    Text(
                        text = "ID: ${portalConfig.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "已選擇",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (portalConfig.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = portalConfig.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // 參數預覽
            if (portalConfig.parameters.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${portalConfig.parameters.size} 個參數",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // 最後使用時間
            if (portalConfig.lastUsed > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "最後使用：${formatLastUsed(portalConfig.lastUsed)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }
                )
            }
        }
    }
}

/**
 * Portal參數編輯對話框
 */
@Composable
private fun PortalParameterEditDialog(
    portalConfig: PortalConfig,
    onSave: (PortalConfig) -> Unit,
    onCancel: () -> Unit
) {
    // 使用現有的PortalEditDialog，但只允許編輯參數
    PortalEditDialog(
        portalConfig = portalConfig,
        onSave = onSave,
        onCancel = onCancel,
        onUpdateParameter = { _, _ -> /* 在這個對話框中處理 */ },
        onAddParameter = { /* 在這個對話框中處理 */ },
        onRemoveParameter = { /* 在這個對話框中處理 */ }
    )
}

private fun formatLastUsed(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60 * 1000 -> "剛剛"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} 分鐘前"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} 小時前"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)} 天前"
        else -> "很久以前"
    }
}
