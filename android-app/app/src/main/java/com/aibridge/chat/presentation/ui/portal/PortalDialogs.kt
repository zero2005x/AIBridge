package com.aibridge.chat.presentation.ui.portal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aibridge.chat.domain.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortalEditDialog(
    portalConfig: PortalConfig,
    onSave: (PortalConfig) -> Unit,
    onCancel: () -> Unit,
    onUpdateParameter: (String, PortalParameter) -> Unit,
    onAddParameter: (PortalParameter) -> Unit,
    onRemoveParameter: (String) -> Unit
) {
    var editedConfig by remember { mutableStateOf(portalConfig) }
    var showAddParameterDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                TopAppBar(
                    title = { 
                        Text(
                            text = if (portalConfig.id.isEmpty()) "新增Portal" else "編輯Portal",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Default.Close, contentDescription = "取消")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { onSave(editedConfig) }
                        ) {
                            Text("保存")
                        }
                    }
                )

                // Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Basic Info
                    item {
                        Card {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "基本信息",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = editedConfig.id,
                                    onValueChange = { editedConfig = editedConfig.copy(id = it) },
                                    label = { Text("Portal ID") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = portalConfig.id.isEmpty() // Only allow editing for new portals
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = editedConfig.name,
                                    onValueChange = { editedConfig = editedConfig.copy(name = it) },
                                    label = { Text("Portal名稱") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = editedConfig.description,
                                    onValueChange = { editedConfig = editedConfig.copy(description = it) },
                                    label = { Text("描述") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    maxLines = 4
                                )
                            }
                        }
                    }

                    // Parameters
                    item {
                        Card {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "參數配置",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    IconButton(
                                        onClick = { showAddParameterDialog = true }
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "新增參數")
                                    }
                                }

                                if (editedConfig.parameters.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "沒有參數，點擊 + 來新增",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    editedConfig.parameters.forEach { (name, parameter) ->
                                        ParameterEditCard(
                                            parameter = parameter,
                                            onUpdate = { updatedParam ->
                                                val updatedParams = editedConfig.parameters.toMutableMap()
                                                updatedParams[name] = updatedParam
                                                editedConfig = editedConfig.copy(parameters = updatedParams)
                                                onUpdateParameter(name, updatedParam)
                                            },
                                            onRemove = {
                                                val updatedParams = editedConfig.parameters.toMutableMap()
                                                updatedParams.remove(name)
                                                editedConfig = editedConfig.copy(parameters = updatedParams)
                                                onRemoveParameter(name)
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Parameter Dialog
    if (showAddParameterDialog) {
        AddParameterDialog(
            onAdd = { parameter ->
                val updatedParams = editedConfig.parameters.toMutableMap()
                updatedParams[parameter.name] = parameter
                editedConfig = editedConfig.copy(parameters = updatedParams)
                onAddParameter(parameter)
                showAddParameterDialog = false
            },
            onCancel = { showAddParameterDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParameterEditCard(
    parameter: PortalParameter,
    onUpdate: (PortalParameter) -> Unit,
    onRemove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = parameter.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = parameter.type.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row {
                    IconButton(
                        onClick = { expanded = !expanded }
                    ) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "收起" else "展開"
                        )
                    }
                    IconButton(
                        onClick = onRemove
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "刪除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = parameter.value,
                    onValueChange = { onUpdate(parameter.copy(value = it)) },
                    label = { Text("默認值") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = if (parameter.type == ParameterType.TEXTAREA) 3 else 1,
                    maxLines = if (parameter.type == ParameterType.TEXTAREA) 5 else 1
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = parameter.description,
                    onValueChange = { onUpdate(parameter.copy(description = it)) },
                    label = { Text("描述") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = parameter.placeholder,
                    onValueChange = { onUpdate(parameter.copy(placeholder = it)) },
                    label = { Text("佔位符") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = parameter.isRequired,
                        onCheckedChange = { onUpdate(parameter.copy(isRequired = it)) }
                    )
                    Text(
                        text = "必填參數",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddParameterDialog(
    onAdd: (PortalParameter) -> Unit,
    onCancel: () -> Unit
) {
    var paramName by remember { mutableStateOf("") }
    var paramType by remember { mutableStateOf(ParameterType.TEXT) }
    var paramDescription by remember { mutableStateOf("") }
    var paramPlaceholder by remember { mutableStateOf("") }
    var isRequired by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("新增參數") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = paramName,
                    onValueChange = { paramName = it },
                    label = { Text("參數名稱") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = paramType.name,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("參數類型") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        ParameterType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    paramType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = paramDescription,
                    onValueChange = { paramDescription = it },
                    label = { Text("描述") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = paramPlaceholder,
                    onValueChange = { paramPlaceholder = it },
                    label = { Text("佔位符") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isRequired,
                        onCheckedChange = { isRequired = it }
                    )
                    Text(
                        text = "必填參數",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (paramName.isNotEmpty()) {
                        onAdd(
                            PortalParameter(
                                name = paramName,
                                value = "",
                                type = paramType,
                                isRequired = isRequired,
                                description = paramDescription,
                                placeholder = paramPlaceholder
                            )
                        )
                    }
                },
                enabled = paramName.isNotEmpty()
            ) {
                Text("新增")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("取消")
            }
        }
    )
}

@Composable
fun PortalDetailDialog(
    portalDetail: PortalDetail,
    onDismiss: () -> Unit,
    onCreateConfig: (PortalDetail) -> Unit
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
                .fillMaxHeight(0.8f)
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
                        text = "Portal 詳情",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "關閉")
                    }
                }

                Divider()

                // Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        // Basic Info
                        Column {
                            Text(
                                text = portalDetail.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ID: ${portalDetail.id}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            if (portalDetail.description.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = portalDetail.description,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    if (portalDetail.category.isNotEmpty()) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Category,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "分類：${portalDetail.category}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    if (portalDetail.tags.isNotEmpty()) {
                        item {
                            Column {
                                Text(
                                    text = "標籤",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(portalDetail.tags) { tag ->
                                        AssistChip(
                                            onClick = { },
                                            label = { Text(tag) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (portalDetail.parameters.isNotEmpty()) {
                        item {
                            Text(
                                text = "參數",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        items(portalDetail.parameters) { param ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = param.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        
                                        Row {
                                            if (param.isRequired) {
                                                AssistChip(
                                                    onClick = { },
                                                    label = { Text("必填") },
                                                    modifier = Modifier.height(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }
                                            AssistChip(
                                                onClick = { },
                                                label = { Text(param.type.name) },
                                                modifier = Modifier.height(24.dp)
                                            )
                                        }
                                    }
                                    
                                    if (param.description.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = param.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    if (param.placeholder.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "提示：${param.placeholder}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (portalDetail.examples.isNotEmpty()) {
                        item {
                            Text(
                                text = "使用示例",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        items(portalDetail.examples) { example ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = example.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    if (example.description.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = example.description,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    
                                    if (example.parameters.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        example.parameters.forEach { (key, value) ->
                                            Text(
                                                text = "$key: $value",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("關閉")
                    }
                    
                    Button(
                        onClick = { onCreateConfig(portalDetail) }
                    ) {
                        Text("創建配置")
                    }
                }
            }
        }
    }
}
