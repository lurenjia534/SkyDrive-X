package com.lurenjia534.skydrivex.ui.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

internal const val CHANNEL_ID: String = "downloads"

fun createDownloadChannel(context: Context) {
    val channel = NotificationChannel(
        CHANNEL_ID,
        "下载",
        NotificationManager.IMPORTANCE_LOW
    ).apply {
        description = "文件下载/上传进度通知"
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
    withCancelAction: Boolean = false,
    smallIconRes: Int = android.R.drawable.stat_sys_download
) {
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(smallIconRes)
        .setContentTitle(title)
        .setOngoing(true)
        .setOnlyAlertOnce(true)

    if (indeterminate) {
        builder.setProgress(0, 0, true)
        builder.setContentText("正在进行…")
    } else if (progress != null && max != null) {
        builder.setProgress(max, progress, false)
        builder.setContentText("${progress * 100 / max}%")
    }

    // 取消动作由调用方决定是否添加（上传/自定义下载）
    if (withCancelAction) {
        val cancelIntent = android.content.Intent(context, CancelDownloadReceiver::class.java).apply {
            action = CancelDownloadReceiver.ACTION_CANCEL
            putExtra(CancelDownloadReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(CancelDownloadReceiver.EXTRA_TITLE, title)
        }
        val pi = android.app.PendingIntent.getBroadcast(
            context,
            notificationId,
            cancelIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
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
        .setContentText(message ?: if (success) "完成" else "失败")
        .setProgress(0, 0, false)

    safeNotify(context, notificationId, builder)
}

fun replaceWithCompletion(
    context: Context,
    oldNotificationId: Int,
    title: String,
    success: Boolean,
    message: String? = null
) {
    val nm = NotificationManagerCompat.from(context)
    try { nm.cancel(oldNotificationId) } catch (_: SecurityException) {}
    val newId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
    completeNotification(context, newId, title, success, message)
}

private fun safeNotify(context: Context, id: Int, builder: NotificationCompat.Builder) {
    if (canPostNotifications(context)) {
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (_: SecurityException) {
            // permission may be revoked during runtime
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

internal fun nextNotificationId(): Int = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
