package com.lurenjia534.skydrivex.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import com.lurenjia534.skydrivex.ui.notification.DownloadRegistry
import com.lurenjia534.skydrivex.ui.notification.TransferTracker

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val entries by TransferTracker.entries.collectAsState()
    val sorted = remember(entries) {
        entries.sortedByDescending { it.startedAt }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(text = "传输管理器", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            val hasFinished = sorted.any { it.status != TransferTracker.Status.RUNNING }
            if (hasFinished) {
                TextButton(onClick = { TransferTracker.clearFinished() }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "清除完成")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("清除完成")
                }
            }
        }
        if (sorted.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "暂无传输任务", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sorted, key = { it.notificationId }) { entry ->
                    TransferItemCard(entry = entry, context = context)
                }
            }
        }
    }
}

@Composable
private fun TransferItemCard(entry: TransferTracker.Entry, context: Context) {
    val typeLabel = when (entry.type) {
        TransferTracker.TransferType.DOWNLOAD_SYSTEM -> "系统下载"
        TransferTracker.TransferType.DOWNLOAD_CUSTOM -> "自定义目录"
        TransferTracker.TransferType.UPLOAD -> "上传"
    }
    val (statusText, statusColor, statusIcon) = when (entry.status) {
        TransferTracker.Status.RUNNING -> {
            val baseText = when (entry.type) {
                TransferTracker.TransferType.UPLOAD -> if (entry.indeterminate || entry.progress == null) "上传中…" else "上传中 ${entry.progress}%"
                else -> if (entry.indeterminate || entry.progress == null) "下载中…" else "下载中 ${entry.progress}%"
            }
            val icon = when (entry.type) {
                TransferTracker.TransferType.UPLOAD -> Icons.Outlined.FileUpload
                else -> Icons.Outlined.Download
            }
            Triple(baseText, MaterialTheme.colorScheme.primary, icon)
        }
        TransferTracker.Status.SUCCESS -> {
            val successText = if (entry.type == TransferTracker.TransferType.UPLOAD) "上传成功" else "下载成功"
            Triple(successText, MaterialTheme.colorScheme.secondary, Icons.Outlined.CheckCircle)
        }
        TransferTracker.Status.FAILED -> {
            val failedText = if (entry.type == TransferTracker.TransferType.UPLOAD) "上传失败" else "下载失败"
            Triple(failedText, MaterialTheme.colorScheme.error, Icons.Outlined.ErrorOutline)
        }
        TransferTracker.Status.CANCELLED -> Triple("已取消", MaterialTheme.colorScheme.tertiary, Icons.Outlined.Close)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusChip(text = statusText, color = statusColor)
                        StatusChip(
                            text = typeLabel,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    if (!entry.message.isNullOrBlank() && entry.status != TransferTracker.Status.RUNNING) {
                        Text(
                            text = entry.message ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (entry.status == TransferTracker.Status.RUNNING) {
                val progressFraction = entry.progress?.coerceIn(0, 100)?.div(100f)
                if (progressFraction != null) {
                    LinearProgressIndicator(progress = progressFraction, modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (entry.status) {
                    TransferTracker.Status.RUNNING -> {
                        if (entry.allowCancel) {
                            TextButton(onClick = { DownloadRegistry.cancel(context, entry.notificationId) }) {
                                Icon(Icons.Outlined.Close, contentDescription = "取消传输")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("取消")
                            }
                        }
                    }
                    TransferTracker.Status.SUCCESS -> {
                        TextButton(onClick = { TransferTracker.remove(entry.notificationId) }) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = "完成")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("完成")
                        }
                    }
                    TransferTracker.Status.FAILED -> {
                        TextButton(onClick = { TransferTracker.remove(entry.notificationId) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "移除记录")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("移除")
                        }
                    }
                    TransferTracker.Status.CANCELLED -> {
                        TextButton(onClick = { TransferTracker.remove(entry.notificationId) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "移除记录")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("移除")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        shape = RoundedCornerShape(999.dp),
        tonalElevation = 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}
