package com.aibridge.chat.presentation.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aibridge.chat.presentation.viewmodel.ChatViewModel
import com.aibridge.chat.presentation.viewmodel.ChatEvent

@Composable
fun ChatScreen(
    windowSizeClass: WindowSizeClass,
    onLogout: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPortalManagement: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val messages by viewModel.currentSessionMessages.collectAsStateWithLifecycle()

    when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            CompactChatScreen(
                uiState = uiState,
                sessions = sessions,
                messages = messages,
                onEvent = viewModel::onEvent,
                onLogout = onLogout,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToPortalManagement = onNavigateToPortalManagement
            )
        }
        WindowWidthSizeClass.Medium -> {
            MediumChatScreen(
                uiState = uiState,
                sessions = sessions,
                messages = messages,
                onEvent = viewModel::onEvent,
                onLogout = onLogout,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToPortalManagement = onNavigateToPortalManagement
            )
        }
        WindowWidthSizeClass.Expanded -> {
            ExpandedChatScreen(
                uiState = uiState,
                sessions = sessions,
                messages = messages,
                onEvent = viewModel::onEvent,
                onLogout = onLogout,
                onNavigateToSettings = onNavigateToSettings
            )
        }
    }

    // 錯誤訊息處理
    uiState.errorMessage?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            // TODO: 顯示 Snackbar 或 Toast
            viewModel.clearError()
        }
    }
    
    // 調試功能 - 僅在調試模式下顯示
    var showDebugDialog by remember { mutableStateOf(false) }
    
    // 可以通過 FloatingActionButton 觸發調試功能
    if (showDebugDialog) {
        com.aibridge.chat.presentation.ui.debug.ChatTestDialog(
            uiState = uiState,
            onEvent = viewModel::onEvent,
            onDismiss = { showDebugDialog = false }
        )
    }
}
