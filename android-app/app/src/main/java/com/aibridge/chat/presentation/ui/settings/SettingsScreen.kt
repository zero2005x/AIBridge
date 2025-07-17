package com.aibridge.chat.presentation.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aibridge.chat.data.repository.AppSettings
import com.aibridge.chat.presentation.viewmodel.SettingsViewModel

/**
 * 設定頁面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 頂部欄
        TopAppBar(
            title = { Text("設定") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            }
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 外觀設定
            item {
                SettingsSection(title = "外觀") {
                    ThemeSettingItem(
                        currentTheme = settings.themeMode,
                        onThemeChange = viewModel::updateThemeMode
                    )
                    
                    SliderSettingItem(
                        title = "字體大小",
                        icon = Icons.Default.TextFields,
                        value = settings.messageFontSize.toFloat(),
                        range = 10f..24f,
                        steps = 6,
                        onValueChange = { viewModel.updateMessageFontSize(it.toInt()) },
                        valueText = "${settings.messageFontSize}sp"
                    )
                }
            }
            
            // 聊天設定
            item {
                SettingsSection(title = "聊天") {
                    SwitchSettingItem(
                        title = "自動滾動",
                        description = "新訊息時自動滾動到底部",
                        icon = Icons.Default.VerticalAlignBottom,
                        checked = settings.autoScroll,
                        onCheckedChange = viewModel::updateAutoScroll
                    )
                    
                    SwitchSettingItem(
                        title = "顯示時間戳記",
                        description = "在訊息中顯示時間",
                        icon = Icons.Default.Schedule,
                        checked = settings.showTimestamps,
                        onCheckedChange = viewModel::updateShowTimestamps
                    )
                    
                    SwitchSettingItem(
                        title = "Markdown 渲染",
                        description = "支援 Markdown 格式顯示",
                        icon = Icons.Default.Code,
                        checked = settings.markdownEnabled,
                        onCheckedChange = viewModel::updateMarkdownEnabled
                    )
                    
                    SwitchSettingItem(
                        title = "程式碼高亮",
                        description = "程式碼區塊語法高亮",
                        icon = Icons.Default.Highlight,
                        checked = settings.codeHighlight,
                        onCheckedChange = viewModel::updateCodeHighlight
                    )
                    
                    SliderSettingItem(
                        title = "歷史記錄上限",
                        icon = Icons.Default.History,
                        value = settings.maxChatHistory.toFloat(),
                        range = 50f..500f,
                        steps = 8,
                        onValueChange = { viewModel.updateMaxChatHistory(it.toInt()) },
                        valueText = "${settings.maxChatHistory} 則"
                    )
                }
            }
            
            // 網路設定
            item {
                SettingsSection(title = "網路") {
                    SliderSettingItem(
                        title = "請求逾時",
                        icon = Icons.Default.Timer,
                        value = settings.requestTimeout.toFloat(),
                        range = 10f..120f,
                        steps = 10,
                        onValueChange = { viewModel.updateRequestTimeout(it.toInt()) },
                        valueText = "${settings.requestTimeout} 秒"
                    )
                    
                    SliderSettingItem(
                        title = "重試次數",
                        icon = Icons.Default.Refresh,
                        value = settings.retryCount.toFloat(),
                        range = 1f..10f,
                        steps = 8,
                        onValueChange = { viewModel.updateRetryCount(it.toInt()) },
                        valueText = "${settings.retryCount} 次"
                    )
                    
                    SwitchSettingItem(
                        title = "自動重試",
                        description = "失敗時自動重試請求",
                        icon = Icons.Default.Autorenew,
                        checked = settings.autoRetry,
                        onCheckedChange = viewModel::updateAutoRetry
                    )
                }
            }
            
            // 安全設定
            item {
                SettingsSection(title = "安全") {
                    SwitchSettingItem(
                        title = "生物識別認證",
                        description = "使用指紋或臉部解鎖",
                        icon = Icons.Default.Fingerprint,
                        checked = settings.biometricAuth,
                        onCheckedChange = viewModel::updateBiometricAuth
                    )
                }
            }
            
            // 備份設定
            item {
                SettingsSection(title = "備份") {
                    SwitchSettingItem(
                        title = "自動備份",
                        description = "定期自動備份聊天記錄",
                        icon = Icons.Default.Backup,
                        checked = settings.autoBackup,
                        onCheckedChange = viewModel::updateAutoBackup
                    )
                    
                    if (settings.autoBackup) {
                        SliderSettingItem(
                            title = "備份頻率",
                            icon = Icons.Default.Schedule,
                            value = settings.backupFrequency.toFloat(),
                            range = 1f..30f,
                            steps = 29,
                            onValueChange = { viewModel.updateBackupFrequency(it.toInt()) },
                            valueText = "${settings.backupFrequency} 天"
                        )
                    }
                }
            }
            
            // 其他設定
            item {
                SettingsSection(title = "其他") {
                    SwitchSettingItem(
                        title = "推送通知",
                        description = "接收應用通知",
                        icon = Icons.Default.Notifications,
                        checked = settings.enableNotifications,
                        onCheckedChange = viewModel::updateEnableNotifications
                    )
                    
                    ClickableSettingItem(
                        title = "重置設定",
                        description = "將所有設定恢復為預設值",
                        icon = Icons.Default.RestartAlt,
                        onClick = {
                            viewModel.resetToDefaults()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun SwitchSettingItem(
    title: String,
    description: String? = null,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SliderSettingItem(
    title: String,
    icon: ImageVector,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    valueText: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            )
            
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            modifier = Modifier.padding(start = 40.dp, top = 4.dp)
        )
    }
}

@Composable
private fun ThemeSettingItem(
    currentTheme: String,
    onThemeChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Palette,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "主題模式",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = when (currentTheme) {
                    "light" -> "淺色"
                    "dark" -> "深色"
                    else -> "跟隨系統"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.ExpandMore, contentDescription = "選擇主題")
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("跟隨系統") },
                    onClick = {
                        onThemeChange("system")
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("淺色") },
                    onClick = {
                        onThemeChange("light")
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("深色") },
                    onClick = {
                        onThemeChange("dark")
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ClickableSettingItem(
    title: String,
    description: String? = null,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        IconButton(onClick = onClick) {
            Icon(Icons.Default.ChevronRight, contentDescription = "點擊")
        }
    }
}
