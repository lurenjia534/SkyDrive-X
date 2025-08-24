package com.lurenjia534.skydrivex.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Dialog to create/share a link for a Drive item.
 *
 * Supports choosing link type (view/edit), scope (anyone/organization), optional expiration days,
 * and optional password (only for personal accounts + anonymous scope).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareLinkDialog(
    isBusiness: Boolean,                // true for OneDrive for Business/SharePoint
    fileName: String?,
    onDismiss: () -> Unit,
    onCreate: (
        type: String,
        scope: String?,
        expirationDays: Int?,
        password: String?
    ) -> Unit
) {
    var type by remember { mutableStateOf("view") } // view | edit
    var scope by remember { mutableStateOf("anonymous") } // anonymous | organization
    var expandedType by remember { mutableStateOf(false) }
    var expandedScope by remember { mutableStateOf(false) }
    var useExpiration by remember { mutableStateOf(false) }
    var expirationDaysText by remember { mutableStateOf("7") }
    var usePassword by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }

    val passwordEnabled = !isBusiness && scope == "anonymous"

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val days = if (useExpiration) expirationDaysText.toIntOrNull() else null
                val pwd = if (usePassword && passwordEnabled && password.isNotEmpty()) password else null
                onCreate(type, scope, days, pwd)
            }) { Text("创建链接") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text("分享：${fileName ?: "文件"}") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("选择链接类型与范围", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))

                // Type
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("链接类型")
                    Row {
                        Text(typeLabel(type))
                        IconButton(onClick = { expandedType = true }) { Icon(Icons.Default.ArrowDropDown, null) }
                        DropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }) {
                            DropdownMenuItem(text = { Text("仅查看") }, onClick = { type = "view"; expandedType = false })
                            DropdownMenuItem(text = { Text("可编辑") }, onClick = { type = "edit"; expandedType = false })
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Scope
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("访问范围")
                    Row {
                        Text(scopeLabel(scope, isBusiness))
                        IconButton(onClick = { expandedScope = true }) { Icon(Icons.Default.ArrowDropDown, null) }
                        DropdownMenu(expanded = expandedScope, onDismissRequest = { expandedScope = false }) {
                            DropdownMenuItem(text = { Text("任何人（匿名）") }, onClick = { scope = "anonymous"; expandedScope = false })
                            if (isBusiness) {
                                DropdownMenuItem(text = { Text("仅组织内用户") }, onClick = { scope = "organization"; expandedScope = false })
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("可选设置", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))

                // Expiration
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = useExpiration, onCheckedChange = { useExpiration = it })
                    Text("设置到期（天数）")
                }
                if (useExpiration) {
                    OutlinedTextField(
                        value = expirationDaysText,
                        onValueChange = { expirationDaysText = it.filter { ch -> ch.isDigit() }.take(3) },
                        singleLine = true,
                        label = { Text("天数") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Password (only for personal + anonymous)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(
                        checked = usePassword && passwordEnabled,
                        onCheckedChange = { usePassword = it },
                        enabled = passwordEnabled
                    )
                    val pwdHint = if (passwordEnabled) "设置密码（仅匿名链接）" else if (isBusiness) "企业版不支持密码" else "组织范围下不支持密码"
                    Text(pwdHint)
                }
                if (usePassword && passwordEnabled) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it.take(100) },
                        singleLine = true,
                        label = { Text("密码") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "提示：‘直接下载’可在文件更多菜单选择‘下载链接’获取，短期有效，不建议长期分享。",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    )
}

private fun typeLabel(type: String) = when (type) {
    "edit" -> "可编辑"
    else -> "仅查看"
}

private fun scopeLabel(scope: String, isBusiness: Boolean) = when (scope) {
    "organization" -> if (isBusiness) "仅组织内用户" else "仅组织（不适用于个人）"
    else -> "任何人（匿名）"
}

