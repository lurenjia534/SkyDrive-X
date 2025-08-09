package com.lurenjia534.skydrivex.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import androidx.hilt.navigation.compose.hiltViewModel
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import com.lurenjia534.skydrivex.viewmodel.FilesViewModel
import java.util.Locale

/**
 * Screen that displays files and folders from the user's drive.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    token: String?,
    modifier: Modifier = Modifier,
    viewModel: FilesViewModel = hiltViewModel<FilesViewModel>(),
) {
    val uiState by viewModel.filesState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = token) {
        token?.let { viewModel.loadRoot(it) }
    }

    LaunchedEffect(uiState.items, uiState.error) {
        uiState.error?.let { message ->
            snackbarHostState.showSnackbar(message)
        } ?: uiState.items?.let {
            snackbarHostState.showSnackbar("加载成功")
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("文件") },
                navigationIcon = {
                    if (uiState.canGoBack) {
                        IconButton(onClick = { token?.let { viewModel.goBack(it) } }, enabled = token != null) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回上一级")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val contentModifier = modifier.fillMaxSize().padding(padding)
        when {
            uiState.isLoading -> {
                FilesLoadingPlaceholder(modifier = contentModifier)
            }

            uiState.error != null -> {
                Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                    Text(text = uiState.error ?: "加载失败")
                }
            }

            uiState.items.isNullOrEmpty() -> {
                Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                    Text(text = "暂无文件")
                }
            }

            else -> {
                LazyColumn(modifier = contentModifier) {
                    item(key = "breadcrumb") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            uiState.path.forEachIndexed { index, crumb ->
                                AssistChip(
                                    onClick = {
                                        if (token != null) {
                                            viewModel.navigateTo(index, token)
                                        }
                                    },
                                    label = { Text(crumb.name) },
                                    enabled = index != uiState.path.lastIndex
                                )
                                if (index != uiState.path.lastIndex) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("/", style = MaterialTheme.typography.labelLarge)
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                    items(uiState.items.orEmpty()) { item ->
                        val isFolder = item.folder != null
                        var expanded by remember { mutableStateOf(false) }

                        Box {
                            ListItem(
                                leadingContent = {
                                    Icon(
                                        imageVector = if (isFolder) Icons.Outlined.Folder else Icons.AutoMirrored.Outlined.InsertDriveFile,
                                        contentDescription = null,
                                    )
                                },
                                headlineContent = { Text(text = item.name ?: "") },
                                supportingContent = {
                                    if (isFolder) {
                                        val count = item.folder.childCount ?: 0
                                        Text("$count 项")
                                    } else {
                                        val sizeText = item.size?.let { formatBytes(it) } ?: ""
                                        if (sizeText.isNotEmpty()) Text(sizeText)
                                    }
                                },
                                trailingContent = {
                                    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                                        IconButton(onClick = { expanded = true }) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "更多操作"
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false },
                                            offset = DpOffset(x = 0.dp, y = 0.dp) // 如需可微调
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("删除") },
                                                onClick = {
                                                    item.id?.let { if (token != null) viewModel.deleteFile(it, token) }
                                                    expanded = false
                                                },
                                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) }
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.clickable(enabled = isFolder && item.id != null) {
                                    if (isFolder && item.id != null && token != null) {
                                        viewModel.loadChildren(item.id, token, item.name ?: "")
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val kb = 1024L
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        bytes >= gb -> "%.2f GB".format(Locale.US, bytes.toDouble() / gb)
        bytes >= mb -> "%.2f MB".format(Locale.US, bytes.toDouble() / mb)
        bytes >= kb -> "%.2f KB".format(Locale.US, bytes.toDouble() / kb)
        else -> "$bytes B"
    }
}

@Composable
private fun FilesLoadingPlaceholder(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(10) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图标占位
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(24.dp)
                        .placeholder(
                            visible = true,
                            highlight = PlaceholderHighlight.shimmer(),
                            shape = MaterialTheme.shapes.small
                        )
                )

                Column(modifier = Modifier.weight(1f)) {
                    // 文件名占位
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(18.dp)
                            .placeholder(
                                visible = true,
                                highlight = PlaceholderHighlight.shimmer(),
                                shape = MaterialTheme.shapes.small
                            )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 次要信息占位（如大小/时间）
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .height(14.dp)
                            .placeholder(
                                visible = true,
                                highlight = PlaceholderHighlight.shimmer(),
                                shape = MaterialTheme.shapes.small
                            )
                    )
                }
            }
        }
    }
}
