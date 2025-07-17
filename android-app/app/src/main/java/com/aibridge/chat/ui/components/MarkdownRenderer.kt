package com.aibridge.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.regex.Pattern

/**
 * Markdown 文本渲染組件
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    if (!isEnabled) {
        // 如果關閉 Markdown，直接顯示純文字
        Text(
            text = markdown,
            modifier = modifier
        )
        return
    }
    
    val annotatedString = remember(markdown) {
        parseMarkdown(markdown)
    }
    
    Text(
        text = annotatedString,
        modifier = modifier
    )
}

/**
 * 程式碼區塊組件
 */
@Composable
fun CodeBlock(
    code: String,
    language: String? = null,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            // 標題列
            if (!language.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = language,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    TextButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(code))
                        },
                        modifier = Modifier.height(24.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "複製",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            
            // 程式碼內容
            Text(
                text = code.trim(),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 行內程式碼組件
 */
@Composable
fun InlineCode(
    code: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = code,
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface
    )
}

/**
 * 解析 Markdown 文本為 AnnotatedString
 */
private fun parseMarkdown(markdown: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = markdown.split("\n")
        var i = 0
        
        while (i < lines.size) {
            val line = lines[i]
            
            when {
                // 程式碼區塊
                line.trimStart().startsWith("```") -> {
                    val language = line.substring(3).trim()
                    i++
                    val codeLines = mutableListOf<String>()
                    
                    while (i < lines.size && !lines[i].trimEnd().endsWith("```")) {
                        codeLines.add(lines[i])
                        i++
                    }
                    
                    if (codeLines.isNotEmpty()) {
                        // 添加程式碼區塊標記（稍後在 UI 中處理）
                        append("CODE_BLOCK_START:$language\n")
                        append(codeLines.joinToString("\n"))
                        append("\nCODE_BLOCK_END\n")
                    }
                }
                
                // 標題
                line.startsWith("#") -> {
                    val level = line.takeWhile { it == '#' }.length
                    val title = line.substring(level).trim()
                    
                    when (level) {
                        1 -> withStyle(SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)) {
                            append(title)
                        }
                        2 -> withStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)) {
                            append(title)
                        }
                        3 -> withStyle(SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)) {
                            append(title)
                        }
                        else -> withStyle(SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold)) {
                            append(title)
                        }
                    }
                    append("\n")
                }
                
                // 普通文本行
                else -> {
                    parseInlineMarkdown(line)
                    if (i < lines.size - 1) append("\n")
                }
            }
            i++
        }
    }
}

/**
 * 解析行內 Markdown 格式
 */
private fun AnnotatedString.Builder.parseInlineMarkdown(text: String) {
    var currentIndex = 0
    
    // 正則表達式模式
    val patterns = listOf(
        // 行內程式碼
        Pattern.compile("`([^`]+)`") to { match: String ->
            withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = Color.Gray.copy(alpha = 0.2f)
                )
            ) {
                append(match.substring(1, match.length - 1))
            }
        },
        
        // 粗體
        Pattern.compile("\\*\\*([^*]+)\\*\\*") to { match: String ->
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(match.substring(2, match.length - 2))
            }
        },
        
        // 斜體
        Pattern.compile("\\*([^*]+)\\*") to { match: String ->
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                append(match.substring(1, match.length - 1))
            }
        },
        
        // 刪除線
        Pattern.compile("~~([^~]+)~~") to { match: String ->
            withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                append(match.substring(2, match.length - 2))
            }
        }
    )
    
    // 查找所有匹配
    val matches = mutableListOf<Triple<Int, Int, (String) -> Unit>>()
    
    for ((pattern, styleApplier) in patterns) {
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            matches.add(Triple(matcher.start(), matcher.end(), styleApplier))
        }
    }
    
    // 按位置排序
    matches.sortBy { it.first }
    
    // 移除重疊的匹配
    val nonOverlapping = mutableListOf<Triple<Int, Int, (String) -> Unit>>()
    for (match in matches) {
        if (nonOverlapping.isEmpty() || match.first >= nonOverlapping.last().second) {
            nonOverlapping.add(match)
        }
    }
    
    // 應用樣式
    for ((start, end, styleApplier) in nonOverlapping) {
        // 添加樣式前的普通文本
        if (start > currentIndex) {
            append(text.substring(currentIndex, start))
        }
        
        // 應用樣式
        styleApplier(text.substring(start, end))
        currentIndex = end
    }
    
    // 添加剩餘的普通文本
    if (currentIndex < text.length) {
        append(text.substring(currentIndex))
    }
}
