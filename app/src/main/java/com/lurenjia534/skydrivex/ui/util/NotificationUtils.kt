package com.lurenjia534.skydrivex.ui.util

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
import androidx.core.app.NotificationManagerCompat

private const val CHANNEL_ID = "downloads"

fun createDownloadChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
