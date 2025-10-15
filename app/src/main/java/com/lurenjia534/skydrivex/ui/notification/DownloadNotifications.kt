package com.lurenjia534.skydrivex.ui.notification

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

/**
 * Download-related notifications and DownloadManager bridging.
 */
fun startSystemDownloadWithNotification(
    context: Context,
    url: String,
    fileName: String,
    description: String = "SkyDriveX 下载"
) {
    val dm = context.getSystemService(DownloadManager::class.java) ?: return
    val request = DownloadManager.Request(url.toUri())
        .setTitle(fileName)
        .setDescription(description)
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
    try {
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
    } catch (_: Throwable) { }

    val downloadId = dm.enqueue(request)
    val nid = (downloadId % Int.MAX_VALUE).toInt()
    TransferTracker.start(
        notificationId = nid,
        title = fileName,
        type = TransferTracker.TransferType.DOWNLOAD_SYSTEM,
        allowCancel = true,
        indeterminate = true
    )
    DownloadRegistry.registerDownloadManager(nid, downloadId)
    DownloadProgressMonitor.start(context, nid, downloadId)

    val appCtx = context.applicationContext
    val receiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id == downloadId) {
                try {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    dm.query(query)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            val status = if (statusIdx >= 0) cursor.getInt(statusIdx) else -1
                            val success = status == DownloadManager.STATUS_SUCCESSFUL
                            DownloadProgressMonitor.stop(nid)
                            if (success) {
                                TransferTracker.markSuccess(nid)
                            } else {
                                TransferTracker.markFailed(nid)
                            }
                        } else {
                            DownloadProgressMonitor.stop(nid)
                            TransferTracker.markFailed(nid)
                        }
                    }
                } catch (e: Exception) {
                    DownloadProgressMonitor.stop(nid)
                    TransferTracker.markFailed(nid)
                } finally {
                    try { appCtx.unregisterReceiver(this) } catch (_: Exception) {}
                    DownloadRegistry.cleanup(nid)
                }
            }
        }
    }
    try {
        ContextCompat.registerReceiver(
            appCtx,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    } catch (_: Exception) { }
}
