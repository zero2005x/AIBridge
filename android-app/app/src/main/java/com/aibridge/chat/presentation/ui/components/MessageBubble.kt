package com.aibridge.chat.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aibridge.chat.domain.model.Message
import com.aibridge.chat.ui.components.MarkdownText
import java.text.SimpleDateFormat
import java.util.*

/**
 * 改進的訊息氣泡組件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageBubble(
    message: Message,
    isMarkdownEnabled: Boolean = true,
    showTimestamp: Boolean = true,
    onCopyClick: ((String) -> Unit)? = null,
    onRetryClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var showMenu by remember { mutableStateOf(false) }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (message.isUser) {
            // 用戶訊息 - 右對齊
            Spacer(modifier = Modifier.weight(1f, fill = false))
            
            UserMessageBubble(
                message = message,
                isMarkdownEnabled = isMarkdownEnabled,
                showTimestamp = showTimestamp,
                onLongClick = { showMenu = true }
            )
        } else {
            // AI 訊息 - 左對齊
            AIMessageBubble(
                message = message,
                isMarkdownEnabled = isMarkdownEnabled,
                showTimestamp = showTimestamp,
                onLongClick = { showMenu = true },
                onRetryClick = onRetryClick
            )
            
            Spacer(modifier = Modifier.weight(1f, fill = false))
        }
    }
    
    // 長按選單
    if (showMenu) {
        MessageContextMenu(
            message = message,
            onDismiss = { showMenu = false },
            onCopy = {
                clipboardManager.setText(AnnotatedString(message.content))
                onCopyClick?.invoke(message.content)
                showMenu = false
            },
            onRetry = onRetryClick?.let { { it(); showMenu = false } }
        )
    }
}

@Composable
private fun UserMessageBubble(
    message: Message,
    isMarkdownEnabled: Boolean,
    showTimestamp: Boolean,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .clickable { onLongClick() },
        shape = RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            MarkdownText(
                markdown = message.content,
                isEnabled = isMarkdownEnabled,
                modifier = Modifier.fillMaxWidth()
            )
            
            if (showTimestamp) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AIMessageBubble(
    message: Message,
    isMarkdownEnabled: Boolean,
    showTimestamp: Boolean,
    onLongClick: () -> Unit,
    onRetryClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // AI 頭像
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = "AI",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSecondary
            )
        }
        
        // 訊息卡片
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clickable { onLongClick() },
            shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                MarkdownText(
                    markdown = message.content,
                    isEnabled = isMarkdownEnabled,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showTimestamp) {
                        Text(
                            text = formatTimestamp(message.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    
                    // 重試按鈕（如果可用）
                    if (onRetryClick != null) {
                        IconButton(
                            onClick = onRetryClick,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "重試",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                // 模型和服務資訊
                if (!message.model.isNullOrBlank() || !message.service.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!message.service.isNullOrBlank()) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = message.service,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        if (!message.model.isNullOrBlank()) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = message.model,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        
                        // Token 數量（如果可用）
                        if (message.tokens != null && message.tokens > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "${message.tokens} tokens",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageContextMenu(
    message: Message,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("訊息選項") },
        text = {
            Column {
                TextButton(
                    onClick = onCopy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("複製文字")
                }
                
                if (onRetry != null && !message.isUser) {
                    TextButton(
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("重新生成")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("關閉")
            }
        }
    )
}

/**
 * 系統訊息組件
 */
@Composable
fun SystemMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 錯誤訊息組件
 */
@Composable
fun ErrorMessage(
    message: String,
    onRetryClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            
            if (onRetryClick != null) {
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = onRetryClick,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("重試")
                }
            }
        }
    }
}

/**
 * 格式化時間戳記
 */
private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
