package com.lurenjia534.skydrivex.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FabActionSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onUploadPhoto: () -> Unit,
    onUploadFiles: () -> Unit,
    onNewFolder: () -> Unit,
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = BottomSheetDefaults.ExpandedShape,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "选择操作", style = MaterialTheme.typography.titleMedium)

            // 上传照片
            ListItem(
                leadingContent = { Icon(Icons.Outlined.Image, contentDescription = null) },
                headlineContent = { Text("上传照片") },
                supportingContent = { Text("使用系统照片选择器") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) {
                        onDismiss()
                        onUploadPhoto()
                    }
            )
            // 上传文件
            ListItem(
                leadingContent = { Icon(Icons.AutoMirrored.Outlined.InsertDriveFile, contentDescription = null) },
                headlineContent = { Text("上传文件") },
                supportingContent = { Text("选择任意类型文件") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) {
                        onDismiss()
                        onUploadFiles()
                    }
            )
            // 新建文件夹
            ListItem(
                leadingContent = { Icon(Icons.Outlined.CreateNewFolder, contentDescription = null) },
                headlineContent = { Text("新建文件夹") },
                supportingContent = { Text("在当前目录创建") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) {
                        onDismiss()
                        onNewFolder()
                    }
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
