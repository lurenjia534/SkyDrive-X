package com.lurenjia534.skydrivex.ui.notification

import android.app.DownloadManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * 轮询系统 DownloadManager 以获取下载进度，并同步到 TransferTracker。
 * 注意：DownloadManager 没有实时回调，只能定时查询。
 */
object DownloadProgressMonitor {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = ConcurrentHashMap<Int, kotlinx.coroutines.Job>()

    fun start(context: Context, notificationId: Int, downloadId: Long) {
        stop(notificationId)
        val appCtx = context.applicationContext
        val dm = appCtx.getSystemService(DownloadManager::class.java) ?: return
        val job = scope.launch {
            val query = DownloadManager.Query().setFilterById(downloadId)
            while (isActive) {
                val cursor = runCatching { dm.query(query) }.getOrNull()
                if (cursor == null) {
                    delay(1_000)
                    continue
                }
                cursor.use {
                    if (!it.moveToFirst()) {
                        return@launch
                    }
                    val statusIdx = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val downloadedIdx = it.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalIdx = it.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val status = if (statusIdx >= 0) it.getInt(statusIdx) else -1
                    val downloaded = if (downloadedIdx >= 0) it.getLong(downloadedIdx) else -1L
                    val total = if (totalIdx >= 0) it.getLong(totalIdx) else -1L
                    if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                        if (total > 0) {
                            TransferTracker.updateProgress(notificationId, 100, indeterminate = false)
                        }
                        return@launch
                    } else {
                        if (total > 0L && downloaded >= 0L) {
                            val percent = ((downloaded * 100) / total).toInt().coerceIn(0, 100)
                            TransferTracker.updateProgress(notificationId, percent, indeterminate = false)
                        } else {
                            TransferTracker.updateProgress(notificationId, null, indeterminate = true)
                        }
                    }
                }
                delay(1_000)
            }
        }
        jobs[notificationId] = job
    }

    fun stop(notificationId: Int) {
        jobs.remove(notificationId)?.cancel()
    }

    fun shutdown() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        scope.cancel()
    }
}
