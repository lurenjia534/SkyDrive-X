package com.lurenjia534.skydrivex.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.lurenjia534.skydrivex.R

object PhotoBackupNotifications {
    private const val CHANNEL_ID = "photo_backup_channel"
    private const val CHANNEL_NAME = "照片备份"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    setShowBadge(false)
                    description = "SkyDriveX 照片同步任务"
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun buildProgress(
        context: Context,
        albumName: String,
        completed: Int,
        total: Int
    ): Notification {
        ensureChannel(context)
        val title = context.getString(R.string.photo_backup_notification_title, albumName)
        val subtitle = if (total > 0) {
            context.getString(R.string.photo_backup_notification_progress, completed, total)
        } else {
            context.getString(R.string.photo_backup_notification_scanning)
        }
        val progress = if (total > 0) completed.coerceAtMost(total) else 0
        val max = if (total > 0) total else 0
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSmallIcon(R.drawable.ic_photo_backup)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(max, progress, total == 0)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun buildCompleted(
        context: Context,
        albumName: String,
        successCount: Int,
        failedCount: Int
    ): Notification {
        ensureChannel(context)
        val title = context.getString(R.string.photo_backup_completed_title, albumName)
        val body = if (failedCount == 0) {
            context.getString(R.string.photo_backup_completed_success, successCount)
        } else {
            context.getString(R.string.photo_backup_completed_partial, successCount, failedCount)
        }
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_photo_backup)
            .setAutoCancel(true)
            .build()
    }
}
