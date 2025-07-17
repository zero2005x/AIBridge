package com.aibridge.chat.presentation.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aibridge.chat.domain.model.ChatSession
import com.aibridge.chat.domain.model.Message
import com.aibridge.chat.presentation.viewmodel.ChatEvent
import com.aibridge.chat.presentation.viewmodel.ChatUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactChatScreen(
    uiState: ChatUiState,
    sessions: List<ChatSession>,
    messages: List<Message>,
    onEvent: (ChatEvent) -> Unit,
    onLogout: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPortalManagement: () -> Unit = {}
) {
    var showSidebar by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    LaunchedEffect(showSidebar) {
        if (showSidebar) {
            drawerState.open()
        } else {
            drawerState.close()
        }
    }

    LaunchedEffect(drawerState.isClosed) {
        if (drawerState.isClosed) {
            showSidebar = false
        }
    }

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp)
            ) {
                ChatSidebar(
                    sessions = sessions,
                    selectedSessionId = uiState.selectedSessionId,
                    onSessionSelect = { onEvent(ChatEvent.SelectSession(it)) },
                    onNewSession = { 
                        onEvent(ChatEvent.CreateNewSession("新對話", "openai", "gpt-3.5-turbo"))
                    },
                    onDeleteSession = { onEvent(ChatEvent.DeleteSession(it)) },
                    onLogout = onLogout,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
        },
        drawerState = drawerState
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 頂部應用欄
            TopAppBar(
                title = { 
                    Text(
                        text = sessions.find { it.id == uiState.selectedSessionId }?.title ?: "AI 聊天",
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { showSidebar = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "菜單")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToPortalManagement) {
                        Icon(Icons.Default.Settings, contentDescription = "Portal管理")
                    }
                    IconButton(onClick = { /* TODO: 實現更多選項 */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多")
                    }
                }
            )
            
            // Portal選擇器
            PortalSelector(
                selectedPortalConfig = uiState.selectedPortalConfig,
                onPortalSelected = { portal ->
                    onEvent(ChatEvent.SelectPortalConfig(portal))
                },
                onPortalParametersChanged = { portal ->
                    onEvent(ChatEvent.UpdatePortalParameters(portal))
                }
            )
            
            // 聊天內容區域
            ChatContentArea(
                messages = messages,
                isLoading = uiState.isLoading,
                onSendMessage = { content ->
                    onEvent(ChatEvent.SendMessage(content))
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MediumChatScreen(
    uiState: ChatUiState,
    sessions: List<ChatSession>,
    messages: List<Message>,
    onEvent: (ChatEvent) -> Unit,
    onLogout: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPortalManagement: () -> Unit = {}
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // 側邊欄
        Surface(
            modifier = Modifier.width(300.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            ChatSidebar(
                sessions = sessions,
                selectedSessionId = uiState.selectedSessionId,
                onSessionSelect = { onEvent(ChatEvent.SelectSession(it)) },
                onNewSession = { 
                    onEvent(ChatEvent.CreateNewSession("新對話", "openai", "gpt-3.5-turbo"))
                },
                onDeleteSession = { onEvent(ChatEvent.DeleteSession(it)) },
                onLogout = onLogout,
                onNavigateToSettings = onNavigateToSettings
            )
        }
        
        // 主要聊天區域
        Column(modifier = Modifier.weight(1f)) {
            // 頂部應用欄
            TopAppBar(
                title = { 
                    Text(
                        text = sessions.find { it.id == uiState.selectedSessionId }?.title ?: "AI 聊天",
                        fontWeight = FontWeight.Medium
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToPortalManagement) {
                        Icon(Icons.Default.Settings, contentDescription = "Portal管理")
                    }
                    IconButton(onClick = { /* TODO: 實現更多選項 */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多")
                    }
                }
            )
            
            // Portal選擇器
            PortalSelector(
                selectedPortalConfig = uiState.selectedPortalConfig,
                onPortalSelected = { portal ->
                    onEvent(ChatEvent.SelectPortalConfig(portal))
                },
                onPortalParametersChanged = { portal ->
                    onEvent(ChatEvent.UpdatePortalParameters(portal))
                }
            )
            
            // 聊天內容區域
            ChatContentArea(
                messages = messages,
                isLoading = uiState.isLoading,
                onSendMessage = { content ->
                    onEvent(ChatEvent.SendMessage(content))
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedChatScreen(
    uiState: ChatUiState,
    sessions: List<ChatSession>,
    messages: List<Message>,
    onEvent: (ChatEvent) -> Unit,
    onLogout: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPortalManagement: () -> Unit = {}
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // 側邊欄
        Surface(
            modifier = Modifier.width(320.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            ChatSidebar(
                sessions = sessions,
                selectedSessionId = uiState.selectedSessionId,
                onSessionSelect = { onEvent(ChatEvent.SelectSession(it)) },
                onNewSession = { 
                    onEvent(ChatEvent.CreateNewSession("新對話", "openai", "gpt-3.5-turbo"))
                },
                onDeleteSession = { onEvent(ChatEvent.DeleteSession(it)) },
                onLogout = onLogout,
                onNavigateToSettings = onNavigateToSettings
            )
        }
        
        // 主要聊天區域
        Column(modifier = Modifier.weight(1f)) {
            // 頂部應用欄
            TopAppBar(
                title = { 
                    Text(
                        text = sessions.find { it.id == uiState.selectedSessionId }?.title ?: "選擇對話開始聊天",
                        fontWeight = FontWeight.Medium
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToPortalManagement) {
                        Icon(Icons.Default.Settings, contentDescription = "Portal管理")
                    }
                    IconButton(onClick = { /* TODO: 實現更多選項 */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多")
                    }
                }
            )
            
            if (uiState.selectedSessionId != null) {
                // Portal選擇器
                PortalSelector(
                    selectedPortalConfig = uiState.selectedPortalConfig,
                    onPortalSelected = { portal ->
                        onEvent(ChatEvent.SelectPortalConfig(portal))
                    },
                    onPortalParametersChanged = { portal ->
                        onEvent(ChatEvent.UpdatePortalParameters(portal))
                    }
                )
                
                // 聊天內容區域
                ChatContentArea(
                    messages = messages,
                    isLoading = uiState.isLoading,
                    onSendMessage = { content ->
                        onEvent(ChatEvent.SendMessage(content))
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                // 空狀態
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "歡迎使用 AI 聊天平台",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "點擊左側創建新對話開始聊天",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
