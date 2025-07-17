package com.aibridge.chat.presentation.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.aibridge.chat.presentation.viewmodel.ChatEvent
import com.aibridge.chat.presentation.viewmodel.ChatUiState

@Composable
fun ChatTestDialog(
    uiState: ChatUiState,
    onEvent: (ChatEvent) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "聊天功能測試",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // 診斷按鈕
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onEvent(ChatEvent.RunDiagnostics) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("運行診斷")
                    }
                    
                    Button(
                        onClick = { onEvent(ChatEvent.TestChatFlow) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("測試聊天流程")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 創建測試會話
                var sessionTitle by remember { mutableStateOf("測試會話") }
                
                Text(
                    text = "創建測試會話",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = sessionTitle,
                    onValueChange = { sessionTitle = it },
                    label = { Text("會話標題") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = {
                        onEvent(ChatEvent.CreateNewSession(
                            title = sessionTitle,
                            service = "openai",
                            model = "gpt-3.5-turbo"
                        ))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("創建會話")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 發送測試消息
                var testMessage by remember { mutableStateOf("你好，這是一個測試消息") }
                
                Text(
                    text = "發送測試消息",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = testMessage,
                    onValueChange = { testMessage = it },
                    label = { Text("測試消息") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = { onEvent(ChatEvent.SendMessage(testMessage)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = testMessage.isNotBlank()
                ) {
                    Text("發送消息")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 狀態顯示
                if (uiState.isLoading) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("處理中...")
                        }
                    }
                }
                
                // 錯誤訊息顯示
                uiState.errorMessage?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "錯誤訊息:",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { onEvent(ChatEvent.ClearError) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("清除錯誤")
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 關閉按鈕
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("關閉")
                }
            }
        }
    }
}
