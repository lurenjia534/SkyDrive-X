package com.lurenjia534.skydrivex.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.microsoft.identity.client.IAccount

/**
 * 底部弹窗：集中展示账户列表，支持设置激活账户、移除账户和新增账户。
 * 所有操作完成后都会自动关闭弹窗。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagerSheet(
    accounts: List<IAccount>,
    activeAccount: IAccount?,
    onSetActive: (IAccount) -> Unit,
    onRemove: (IAccount) -> Unit,
    onAddAccount: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("账户管理", style = MaterialTheme.typography.titleLarge)
            if (accounts.isEmpty()) {
                Text(
                    text = "尚未添加任何账户，点击下方按钮添加新账户。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        onAddAccount()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("添加账户")
                }
            } else {
                accounts.forEach { account ->
                    AccountListItem(
                        account = account,
                        isActive = account.id == activeAccount?.id,
                        onSetActive = {
                            onSetActive(account)
                            onDismiss()
                        },
                        onRemove = {
                            onRemove(account)
                            onDismiss()
                        }
                    )
                }
                Divider()
                Button(
                    onClick = {
                        onAddAccount()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("添加账户")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * 单行账户信息，提供设为激活与移除操作。
 */
@Composable
private fun AccountListItem(
    account: IAccount,
    isActive: Boolean,
    onSetActive: () -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = account.username,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        val secondaryText = account.claims?.get("name") as? String
        if (!secondaryText.isNullOrBlank()) {
            Text(
                text = secondaryText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = onSetActive,
                enabled = !isActive,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isActive) "已为激活账户" else "设为激活")
            }
            TextButton(
                onClick = onRemove,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "移除",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
