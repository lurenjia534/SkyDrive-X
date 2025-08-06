package com.lurenjia534.skydrivex.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.lurenjia534.skydrivex.viewmodel.FilesViewModel

/**
 * Screen that displays files and folders from the user's drive.
 */
@Composable
fun FilesScreen(
    token: String?,
    modifier: Modifier = Modifier,
    viewModel: FilesViewModel = hiltViewModel<FilesViewModel>(),
) {
    val uiState by viewModel.filesState.collectAsState()

    LaunchedEffect(token) {
        if (token != null) {
            viewModel.loadRoot(token)
        }
    }

    when {
        uiState.isLoading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        uiState.error != null -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = uiState.error ?: "加载失败")
            }
        }

        uiState.items.isNullOrEmpty() -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "暂无文件")
            }
        }

        else -> {
            LazyColumn(modifier = modifier.fillMaxSize()) {
                items(uiState.items!!) { item ->
                    val isFolder = item.folder != null
                    ListItem(
                        headlineContent = { Text(text = item.name ?: "") },
                        leadingContent = {
                            Icon(
                                imageVector = if (isFolder) Icons.Outlined.Folder else Icons.Outlined.InsertDriveFile,
                                contentDescription = null,
                            )
                        },
                        modifier = Modifier.clickable(enabled = isFolder && item.id != null) {
                            if (isFolder && item.id != null && token != null) {
                                viewModel.loadChildren(item.id, token)
                            }
                        },
                    )
                }
            }
        }
    }
}

