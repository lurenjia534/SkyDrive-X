package com.lurenjia534.skydrivex.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun RenameItemDialog(
    currentName: String?,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit
) {
    var text by remember(currentName) { mutableStateOf(currentName ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("新名称") }
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val nn = text.trim()
                if (nn.isNotEmpty()) onConfirm(nn)
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

