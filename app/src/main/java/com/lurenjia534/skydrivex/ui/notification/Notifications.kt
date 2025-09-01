package com.lurenjia534.skydrivex.ui.notification

/**
 * Notifications helpers for downloads/uploads.
 * - Channel management
 * - Progress/indeterminate updates
 * - Completion replacement to avoid OEM stuck-notification issues
 * - System DownloadManager bridging to app-managed notifications
 */

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import java.util.concurrent.atomic.AtomicBoolean
import androidx.core.app.NotificationManagerCompat
import android.app.DownloadManager
import android.content.IntentFilter
import android.os.Environment
import androidx.core.net.toUri
import com.lurenjia534.skydrivex.ui.notification.CancelDownloadReceiver
import com.lurenjia534.skydrivex.ui.notification.DownloadRegistry

private const val CHANNEL_ID = "downloads"

fun createDownloadChannel(context: Context) {
    val channel = NotificationChannel(
        CHANNEL_ID,
        "下载",
        NotificationManager.IMPORTANCE_LOW
    ).apply {
        description = "文件下载进度通知"
    }
    val manager = context.getSystemService(NotificationManager::class.java)
    manager?.createNotificationChannel(channel)
}

fun showOrUpdateProgress(
    context: Context,
    notificationId: Int,
    title: String,
    progress: Int?,
    max: Int?,
    indeterminate: Boolean,
    withCancelAction: Boolean = false
) {
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle(title)
        .setOngoing(true)
        .setOnlyAlertOnce(true)

    if (indeterminate) {
        builder.setProgress(0, 0, true)
        builder.setContentText("正在下载…")
    } else if (progress != null && max != null) {
        builder.setProgress(max, progress, false)
        builder.setContentText("${progress * 100 / max}%")
    }

    if (withCancelAction) {
        val cancelIntent = Intent(context, CancelDownloadReceiver::class.java).apply {
            action = CancelDownloadReceiver.ACTION_CANCEL
            putExtra(CancelDownloadReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(CancelDownloadReceiver.EXTRA_TITLE, title)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            notificationId,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(0, "取消", pi)
    }

    safeNotify(context, notificationId, builder)
}

fun completeNotification(
    context: Context,
    notificationId: Int,
    title: String,
    success: Boolean,
    message: String? = null
) {
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(if (success) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error)
        .setContentTitle(title)
        .setOngoing(false)
        .setAutoCancel(true)
        .setContentText(message ?: if (success) "下载完成" else "下载失败")
        .setProgress(0, 0, false)

    safeNotify(context, notificationId, builder)
}

// Some OEMs stick at 100% if we reuse the same notification.
// Cancel the old progress notification first, then post a fresh completion notification.
fun replaceWithCompletion(
    context: Context,
    oldNotificationId: Int,
    title: String,
    success: Boolean,
    message: String? = null
) {
    val nm = NotificationManagerCompat.from(context)
    try { nm.cancel(oldNotificationId) } catch (_: SecurityException) {}
    // Use a new ID to avoid OEM caching issues
    val newId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
    completeNotification(context, newId, title, success, message)
}

private fun safeNotify(context: Context, id: Int, builder: NotificationCompat.Builder) {
    if (canPostNotifications(context)) {
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (_: SecurityException) {
            // Silently ignore when permission was revoked at runtime
        }
    }
}

private fun canPostNotifications(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= 33) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}

// ----------------------- Convenience helpers for uploads -----------------------

private fun nextNotificationId(): Int = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()

/**
 * Start an indeterminate, cancellable upload notification for small/unknown-size uploads.
 * Returns Pair(notificationId, cancelFlag).
 */
fun beginIndeterminateUpload(context: Context, title: String): Pair<Int, AtomicBoolean> {
    createDownloadChannel(context)
    val id = nextNotificationId()
    val cancel = AtomicBoolean(false)
    showOrUpdateProgress(context, id, title, null, null, true, withCancelAction = true)
    DownloadRegistry.registerCustom(id, cancel)
    return id to cancel
}

/**
 * Start a progress-based, cancellable upload notification (0-100).
 * Returns Pair(notificationId, cancelFlag).
 */
fun beginProgressUpload(context: Context, title: String, initialPercent: Int = 0): Pair<Int, AtomicBoolean> {
    createDownloadChannel(context)
    val id = nextNotificationId()
    val cancel = AtomicBoolean(false)
    showOrUpdateProgress(context, id, title, initialPercent, 100, false, withCancelAction = true)
    DownloadRegistry.registerCustom(id, cancel)
    return id to cancel
}

/** Update upload progress using uploaded/total in bytes, mapped to 0-100. */
fun updateUploadProgress(context: Context, id: Int, title: String, uploaded: Long, total: Long) {
    val pct = if (total > 0) ((uploaded * 100.0) / total).toInt().coerceIn(0, 100) else 0
    showOrUpdateProgress(context, id, title, pct, 100, false, withCancelAction = true)
}

/** Finish an upload notification and cleanup registry. */
fun finishUpload(context: Context, id: Int, title: String, success: Boolean, message: String? = null) {
    val msg = message ?: if (success) "上传完成" else "上传失败"
    replaceWithCompletion(context, id, title, success, msg)
    DownloadRegistry.cleanup(id)
}

/**
 * Start a system DownloadManager download with an app-managed, cancellable notification.
 * Shows an indeterminate progress notification and replaces it with completion on finish.
 * Automatically unregisters the receiver after completion.
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
    } catch (_: Throwable) {
        // Best effort: some devices may restrict this; DM fallback still works with default destination
    }
    val downloadId = dm.enqueue(request)
    val nid = (downloadId % Int.MAX_VALUE).toInt()
    createDownloadChannel(context)
    showOrUpdateProgress(context, nid, fileName, null, null, true, withCancelAction = true)
    DownloadRegistry.registerDownloadManager(nid, downloadId)

    // Register a one-off receiver to translate DM completion into our app notification
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
    } catch (_: Exception) {
        // If registration fails, still rely on DM system notification; replace ours after some time
    }
}
