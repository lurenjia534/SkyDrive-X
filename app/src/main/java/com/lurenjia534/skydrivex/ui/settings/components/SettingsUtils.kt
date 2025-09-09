package com.lurenjia534.skydrivex.ui.settings.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

/** Utility: human-readable file size. */
fun formatBytes(bytes: Long): String {
    val kb = 1024L
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        bytes >= gb -> "%.2f GB".format(java.util.Locale.US, bytes.toDouble() / gb)
        bytes >= mb -> "%.2f MB".format(java.util.Locale.US, bytes.toDouble() / mb)
        bytes >= kb -> "%.2f KB".format(java.util.Locale.US, bytes.toDouble() / kb)
        else -> "$bytes B"
    }
}

/** Utility: pretty print SAF tree URI. */
fun formatTreeUri(uri: String?): String {
    if (uri.isNullOrBlank()) return "未选择目录"
    return try {
        val u = uri.toUri()
        val last = u.lastPathSegment ?: return uri
        val decoded = java.net.URLDecoder.decode(last, "UTF-8")
        val core = decoded.substringAfter("tree/", decoded)
        val path = core.substringAfter(":", core)
        val prefix = if (core.startsWith("primary:")) "内部存储/" else ""
        prefix + path
    } catch (_: Exception) {
        uri
    }
}

/**
 * A ListItem that supports long-press to copy the supporting text via a dropdown menu.
 */
@Composable
fun CopyableListItem(
    headline: String,
    supporting: String,
    trailing: (@Composable () -> Unit)? = null,
    onCopy: (String) -> Unit
) {
    var expanded = remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = { if (supporting.isNotBlank()) expanded.value = true })
    ) {
        ListItem(
            headlineContent = { Text(headline) },
            supportingContent = { Text(supporting) },
            trailingContent = trailing
        )
        DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
            DropdownMenuItem(text = { Text("复制") }, onClick = {
                expanded.value = false
                onCopy(supporting)
            })
        }
    }
}

/**
 * A ListItem that renders custom supporting content but still supports long-press copy with given text.
 */
@Composable
fun CopyableCustomItem(
    headline: String,
    copyText: String,
    onCopy: (String) -> Unit,
    content: @Composable () -> Unit
) {
    var expanded = remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = { if (copyText.isNotBlank()) expanded.value = true })
    ) {
        ListItem(
            headlineContent = { Text(headline) },
            supportingContent = content
        )
        DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
            DropdownMenuItem(text = { Text("复制") }, onClick = {
                expanded.value = false
                onCopy(copyText)
            })
        }
    }
}

