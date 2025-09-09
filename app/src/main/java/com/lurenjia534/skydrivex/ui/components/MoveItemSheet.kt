package com.lurenjia534.skydrivex.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lurenjia534.skydrivex.ui.state.Breadcrumb
import com.lurenjia534.skydrivex.ui.viewmodel.FolderPickerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveItemSheet(
    token: String,
    visible: Boolean,
    initialName: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (targetFolderId: String, newName: String?) -> Unit,
    viewModel: FolderPickerViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uiState by viewModel.state.collectAsState()
    var newName by remember { mutableStateOf(initialName ?: "") }

    LaunchedEffect(visible) {
        if (visible) viewModel.start(token)
    }

    if (!visible) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row {
                    IconButton(onClick = { viewModel.goBack(token) }, enabled = uiState.canGoBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                    Text(text = "选择目标文件夹", style = MaterialTheme.typography.titleMedium)
                }
                TextButton(onClick = {
                    onConfirm(viewModel.currentFolderId(), newName.ifBlank { null })
                    onDismiss()
                }) { Text("选择此位置") }
            }

            BreadcrumbBar(path = uiState.path, onNavigate = { index ->
                // 简化：通过多次 goBack 退回到指定层级
                val steps = uiState.path.lastIndex - index
                repeat(steps) { viewModel.goBack(token) }
            })

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                singleLine = true,
                label = { Text("新名称（可选）") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            when {
                uiState.isLoading -> {
                    Text("加载中…")
                }
                uiState.error != null -> {
                    Text(uiState.error ?: "错误", color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { viewModel.start(token) }) { Text("重试") }
                }
                uiState.items.isNullOrEmpty() -> {
                    Text("此处没有子文件夹")
                }
                else -> {
                    LazyColumn {
                        items(uiState.items.orEmpty()) { item ->
                            ListItem(
                                leadingContent = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                                headlineContent = { Text(item.name ?: item.id.orEmpty()) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = (item.id != null)) {
                                        item.id?.let { viewModel.navigateInto(it, token, item.name ?: it) }
                                    }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消") }
                Button(onClick = {
                    onConfirm(viewModel.currentFolderId(), newName.ifBlank { null })
                    onDismiss()
                }) { Text("移动") }
            }
            Spacer(Modifier.height(8.dp))
            Text("提示：移动到根目录需要真实根ID，本选择器已自动使用根ID。", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun BreadcrumbBar(
    path: List<Breadcrumb>,
    onNavigate: (index: Int) -> Unit
) {
    // 简版：横向文本+返回按钮已覆盖常用回退；这里用行列方式平铺
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            path.forEachIndexed { index, crumb ->
                Text(
                    text = (crumb.name.ifEmpty { crumb.id }),
                    color = if (index == path.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable(enabled = index != path.lastIndex) { onNavigate(index) }
                )
                if (index != path.lastIndex) Text("/")
            }
        }
    }
}

