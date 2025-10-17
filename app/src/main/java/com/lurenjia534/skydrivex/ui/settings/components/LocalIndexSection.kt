package com.lurenjia534.skydrivex.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("本地索引", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "在后台增量同步 OneDrive 元数据，离线时亦可快速搜索文件。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = state.enabled, onCheckedChange = onEnabledChange)
            }

            if (state.enabled) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ToggleItem(
                        title = "仅在 Wi‑Fi 下同步",
                        description = "避免使用移动数据执行索引任务。",
                        checked = state.wifiOnly,
                        onCheckedChange = onWifiOnlyChange
                    )
                    ToggleItem(
                        title = "仅在充电时运行",
                        description = "连接电源后再执行索引，以节省电量。",
                        checked = state.chargeOnly,
                        onCheckedChange = onChargeOnlyChange
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatChip(label = "索引条目", value = state.indexedCount.toString(), modifier = Modifier.fillMaxWidth())
                StatChip(label = "上次同步", value = state.lastSyncText, modifier = Modifier.fillMaxWidth())
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onClearClicked,
                    enabled = state.enabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("清空索引")
                }
                FilledTonalButton(
                    onClick = onRebuildNow,
                    enabled = state.enabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("立即重建")
                }
            }

            InfoFooter()
        }
    }
}

@Composable
private fun ToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        tonalElevation = 3.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun InfoFooter() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "当前仅缓存 OneDrive 元数据，不会下载文件内容；索引依赖 Microsoft Graph delta，同步失败时可重试。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}
