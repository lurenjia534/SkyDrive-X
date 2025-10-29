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
                val typeLabel = when {
                    details?.folder != null -> "文件夹"
                    details?.name?.endsWith(".apk", ignoreCase = true) == true -> "Android 安装包 (客户端基于文件扩展名判断)"
                    else -> {
                        val mt = details?.file?.mimeType
                        if (mt.isNullOrBlank()) "文件" else "文件 ($mt)"
                    }
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

                // 图片 / 照片（EXIF）属性
                val img = details?.image
                val exif = details?.photo
                if (img != null || exif != null) {
                    Spacer(Modifier.height(16.dp))
                    Text("图片", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(Modifier.height(8.dp))

                    img?.let { i ->
                        val res = if (i.width != null && i.height != null) "${i.width} × ${i.height} 像素" else null
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
                    }

                    exif?.takenDateTime?.let { t ->
                        OutlinedTextField(
                            value = formatIso(t),
                            onValueChange = {},
                            label = { Text("拍摄时间") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    val camera = buildString {
                        val list = mutableListOf<String>()
                        exif?.cameraMake?.takeIf { it.isNotBlank() }?.let { list.add(it) }
                        exif?.cameraModel?.takeIf { it.isNotBlank() }?.let { list.add(it) }
                        append(list.joinToString(" "))
                    }.ifBlank { null }
                    camera?.let {
                        OutlinedTextField(
                            value = it,
                            onValueChange = {},
                            label = { Text("相机") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    val exposure = run {
                        val num = exif?.exposureNumerator
                        val den = exif?.exposureDenominator
                        if (num != null && den != null && den > 0) {
                            // 传统摄影常用 1/den 表示
                            if (num == 1.0) "1/${den.toInt()} s" else "${num}/${den} s"
                        } else null
                    }
                    val optics = buildString {
                        val parts = mutableListOf<String>()
                        exif?.fNumber?.let { parts.add("f/${trimDouble(it)}") }
                        exif?.focalLength?.let { parts.add("${trimDouble(it)} mm") }
                        exif?.iso?.let { parts.add("ISO $it") }
                        exposure?.let { parts.add(it) }
                        append(parts.joinToString(" · "))
                    }.ifBlank { null }
                    optics?.let {
                        OutlinedTextField(
                            value = it,
                            onValueChange = {},
                            label = { Text("拍摄参数") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        // 无需额外间距，紧接着可能是提示或视频区块
                    }

                    // 提示：部分字段仅在 OneDrive 个人版可用
                    val isImageType = (details.file?.mimeType?.startsWith("image/") == true)
                    val exifSparse = (
                        exif == null || (
                            exif.cameraMake == null &&
                            exif.cameraModel == null &&
                            exif.fNumber == null &&
                            exif.focalLength == null &&
                            exif.iso == null &&
                            exif.exposureNumerator == null &&
                            exif.exposureDenominator == null &&
                            exif.orientation == null
                        )
                    )
                    if (isImageType && exifSparse) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "提示：部分图片元数据仅在 OneDrive 个人版可用；企业版通常仅返回拍摄时间，且 image 宽高可能缺失。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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

private fun trimDouble(value: Double): String {
    // 简洁化小数显示，如 2.0 -> 2, 2.50 -> 2.5
    val s = String.format(Locale.getDefault(), "%.2f", value)
    return s.trimEnd('0').trimEnd('.')
}
