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
    createDownloadChannel(context)
    showOrUpdateProgress(
        context = context,
        notificationId = nid,
        title = fileName,
        progress = null,
        max = null,
        indeterminate = true,
        withCancelAction = true,
        smallIconRes = android.R.drawable.stat_sys_download
    )
    DownloadRegistry.registerDownloadManager(nid, downloadId)

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
                            replaceWithCompletion(appCtx, nid, fileName, success)
                        } else {
                            replaceWithCompletion(appCtx, nid, fileName, false)
                        }
                    }
                } catch (_: Exception) {
                    replaceWithCompletion(appCtx, nid, fileName, false)
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
