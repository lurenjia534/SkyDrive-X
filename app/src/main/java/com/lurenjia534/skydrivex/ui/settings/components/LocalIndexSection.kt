package com.lurenjia534.skydrivex.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lurenjia534.skydrivex.ui.settings.LocalIndexSettingsViewModel

@Composable
fun LocalIndexSection(
    state: LocalIndexSettingsViewModel.UiState,
    onEnabledChange: (Boolean) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit,
    onChargeOnlyChange: (Boolean) -> Unit,
    onClearClicked: () -> Unit,
    onRebuildNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = CardDefaults.shape,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingToggleRow(
                    title = "启用本地索引",
                    description = "在后台增量同步 OneDrive 元数据以供快速搜索",
                    checked = state.enabled,
                    onCheckedChange = onEnabledChange
                )

                SettingToggleRow(
                    title = "仅 Wi‑Fi 同步",
                    description = "避免使用流量进行索引更新",
                    checked = state.wifiOnly,
                    onCheckedChange = onWifiOnlyChange,
                    enabled = state.enabled
                )

                SettingToggleRow(
                    title = "仅充电时运行",
                    description = "只有在充电时才执行索引任务",
                    checked = state.chargeOnly,
                    onCheckedChange = onChargeOnlyChange,
                    enabled = state.enabled
                )

                Text(
                    text = "已索引条目：${state.indexedCount}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "上次同步：${state.lastSyncText}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onClearClicked,
                        enabled = state.enabled
                    ) {
                        Text("清空索引")
                    }
                    Button(
                        onClick = onRebuildNow,
                        enabled = state.enabled
                    ) {
                        Text("立即重建")
                    }
                }

                Text(
                    text = "索引任务会调用 Microsoft Graph delta API，当前仅缓存 OneDrive 元数据，不会下载文件内容。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
