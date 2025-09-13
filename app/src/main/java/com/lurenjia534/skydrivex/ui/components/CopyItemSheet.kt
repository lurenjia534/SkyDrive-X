package com.lurenjia534.skydrivex.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Folder
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.lurenjia534.skydrivex.ui.state.Breadcrumb
import com.lurenjia534.skydrivex.ui.viewmodel.FolderPickerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopyItemSheet(
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

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row {
                    IconButton(onClick = { viewModel.goBack(token) }, enabled = uiState.canGoBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                    Text(text = "选择目标文件夹（复制）")
                }
                TextButton(onClick = {
                    onConfirm(viewModel.currentFolderId(), newName.ifBlank { null })
                    onDismiss()
                }) { Text("复制到此处") }
            }

            BreadcrumbBar(path = uiState.path, onNavigate = { index ->
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
                uiState.isLoading -> Text("加载中…")
                uiState.error != null -> {
                    Text(uiState.error ?: "错误")
                    TextButton(onClick = { viewModel.start(token) }) { Text("重试") }
                }
                uiState.items.isNullOrEmpty() -> Text("此处没有子文件夹")
                else -> {
                    androidx.compose.foundation.lazy.LazyColumn {
                        items(uiState.items.orEmpty()) { item ->
                            ListItem(
                                leadingContent = { Icon(Icons.Outlined.Folder, null) },
                                headlineContent = { Text(item.name ?: item.id.orEmpty()) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickableSafe(enabled = (item.id != null)) {
                                        item.id?.let {
                                            viewModel.navigateInto(
                                                it,
                                                token,
                                                item.name ?: it
                                            )
                                        }
                                    }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(onClick = {
                    onConfirm(viewModel.currentFolderId(), newName.ifBlank { null })
                    onDismiss()
                }) { Text("复制", fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(8.dp))
            Text("提示：复制为异步操作，完成后文件列表会自动刷新。",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun BreadcrumbBar(
    path: List<Breadcrumb>,
    onNavigate: (index: Int) -> Unit
) {
    Column(Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            path.forEachIndexed { index, crumb ->
                Text(
                    text = (crumb.name.ifEmpty { crumb.id }),
                    modifier = Modifier.clickableSafe(enabled = index != path.lastIndex) {
                        onNavigate(index)
                    }
                )
                if (index != path.lastIndex) Text("/")
            }
        }
    }
}

private fun Modifier.clickableSafe(enabled: Boolean, onClick: () -> Unit): Modifier =
    if (enabled) this.then(Modifier.clickable { onClick() }) else this
