package com.lurenjia534.skydrivex.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lurenjia534.skydrivex.data.model.driveitem.DriveItemDto
import com.lurenjia534.skydrivex.ui.settings.components.formatBytes
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PropertiesDialog(
    name: String?,
    details: DriveItemDto?,
    onDismiss: () -> Unit
) {
    val title = "\"${name ?: (details?.name ?: "文件")}\"属性"
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("确定") } },
        title = { Text(title) },
        text = {
            val scroll = rememberScrollState()
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(scroll)) {
                Text("基本", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.height(8.dp))

                // 名称
                OutlinedTextField(
                    value = details?.name ?: name.orEmpty(),
                    onValueChange = {},
                    label = { Text("名称") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // 父路径
                val parentPath = details?.parentReference?.path
                OutlinedTextField(
                    value = parentPath ?: "根目录",
                    onValueChange = {},
                    label = { Text("父文件夹") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // 类型
                val typeLabel = if (details?.folder != null) {
                    "文件夹"
                } else {
                    val mt = details?.file?.mimeType
                    if (mt.isNullOrBlank()) "文件" else "文件 (" + mt + ")"
                }
                OutlinedTextField(
                    value = typeLabel,
                    onValueChange = {},
                    label = { Text("类型") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // 内容/大小
                val contentLabel = if (details?.folder != null) {
                    val count = details.folder.childCount ?: 0
                    "$count 项"
                } else {
                    details?.size?.let { "大小 " + formatBytes(it) } ?: ""
                }
                if (contentLabel.isNotBlank()) {
                    OutlinedTextField(
                        value = contentLabel,
                        onValueChange = {},
                        label = { Text("内容") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // 最后修改
                details?.lastModifiedDateTime?.let { ts ->
                    OutlinedTextField(
                        value = formatIso(ts),
                        onValueChange = {},
                        label = { Text("最后修改") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 媒体（视频）属性
                details?.video?.let { v ->
                    Spacer(Modifier.height(16.dp))
                    Text("媒体", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(Modifier.height(8.dp))

                    // 分辨率
                    val res = if (v.width != null && v.height != null) "${v.width} × ${v.height} 像素" else null
                    res?.let {
                        OutlinedTextField(
                            value = it,
                            onValueChange = {},
                            label = { Text("分辨率") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // 帧率
                    v.frameRate?.let { fr ->
                        val frLabel = String.format(Locale.getDefault(), "%.2f fps", fr)
                        OutlinedTextField(
                            value = frLabel,
                            onValueChange = {},
                            label = { Text("帧率") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // 码率（以 kbps 显示）
                    v.bitrate?.let { br ->
                        val kbps = if (br > 0) br / 1000 else br
                        OutlinedTextField(
                            value = "$kbps kbps",
                            onValueChange = {},
                            label = { Text("码率") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // 时长（假定毫秒）
                    v.duration?.let { durMs ->
                        OutlinedTextField(
                            value = formatDurationMs(durMs),
                            onValueChange = {},
                            label = { Text("时长") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // 编码 FourCC
                    v.fourCC?.takeIf { it.isNotBlank() }?.let { fourcc ->
                        OutlinedTextField(
                            value = fourcc,
                            onValueChange = {},
                            label = { Text("编码 FourCC") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // 音频相关（如存在）
                    val audioSummary = buildString {
                        val parts = mutableListOf<String>()
                        v.audioChannels?.let { parts.add("${it} 声道") }
                        v.audioSamplesPerSecond?.let { parts.add("${it} Hz") }
                        v.audioBitsPerSample?.let { parts.add("${it} bit") }
                        v.audioFormat?.let { parts.add(it) }
                        append(parts.joinToString(" · "))
                    }.ifBlank { null }
                    audioSummary?.let {
                        OutlinedTextField(
                            value = it,
                            onValueChange = {},
                            label = { Text("音频") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    )
}

private fun formatIso(iso: String): String = try {
    val instant = Instant.parse(iso)
    val formatter = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm:ss", Locale.getDefault())
    instant.atZone(ZoneId.systemDefault()).format(formatter)
} catch (_: Exception) { iso }

private fun formatDurationMs(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val s = (totalSeconds % 60).toInt()
    val m = ((totalSeconds / 60) % 60).toInt()
    val h = (totalSeconds / 3600).toInt()
    return if (h > 0) String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
    else String.format(Locale.getDefault(), "%d:%02d", m, s)
}
