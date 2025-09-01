package com.lurenjia534.skydrivex.ui.util

import android.content.Context
import android.provider.DocumentsContract
import androidx.core.net.toUri
import java.net.HttpURLConnection
import java.net.URL

/**
 * 将下载链接保存到用户通过 SAF 选择的目录下。
 * 返回 true 表示保存成功；失败会尝试删除部分写入的临时文件。
 */
fun saveToTree(
    context: Context,
    treeUriString: String,
    fileName: String,
    downloadUrl: String,
    totalBytes: Long?,
    onProgress: (downloaded: Long, total: Long) -> Unit,
    cancelFlag: java.util.concurrent.atomic.AtomicBoolean
): Boolean {
    val resolver = context.contentResolver
    val treeUri = treeUriString.toUri()
    val parentDoc = DocumentsContract.buildDocumentUriUsingTree(
        treeUri,
        DocumentsContract.getTreeDocumentId(treeUri)
    )
    val newDocUri = try {
        DocumentsContract.createDocument(
            resolver,
            parentDoc,
            "application/octet-stream",
            fileName
        )
    } catch (e: Exception) {
        null
    } ?: return false

    return try {
        resolver.openOutputStream(newDocUri)?.use { out ->
            val url = URL(downloadUrl)
            (url.openConnection() as HttpURLConnection).let { conn ->
                conn.requestMethod = "GET"
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                conn.inputStream.use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        if (cancelFlag.get()) {
                            throw InterruptedException("cancelled")
                        }
                        out.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, totalBytes ?: -1L)
                    }
                    out.flush()
                }
                conn.disconnect()
            }
        }
        true
    } catch (e: Exception) {
        // try delete partial
        try { DocumentsContract.deleteDocument(resolver, newDocUri) } catch (_: Exception) {}
        false
    }
}

