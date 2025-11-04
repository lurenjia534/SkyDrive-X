package com.lurenjia534.skydrivex.work

import android.content.Context
import android.content.pm.PackageManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.lurenjia534.skydrivex.auth.GraphTokenProvider
import com.lurenjia534.skydrivex.data.repository.PhotoBackupManager
import androidx.hilt.work.WorkerAssistedFactory
import com.lurenjia534.skydrivex.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class PhotoBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val tokenProvider: GraphTokenProvider,
    private val backupManager: PhotoBackupManager
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_BUCKET_ID = "bucketId"
        const val KEY_ALBUM_NAME = "albumName"
        private const val NOTIFICATION_ID = 0x50484f // 'PHO'
        private const val CHANNEL_ID = "photo_backup"
        private const val CHANNEL_NAME = "照片备份"
    }

    private var lastCompleted = 0
    private var lastTotal = 0

    override suspend fun doWork(): Result {
        val bucketId = inputData.getString(KEY_BUCKET_ID) ?: return Result.failure()
        val albumName = inputData.getString(KEY_ALBUM_NAME) ?: return Result.failure()

        setForeground(createForegroundInfo(albumName, 0, 0))

        return try {
            val token = tokenProvider.getAccessToken()
            val result = backupManager.backupAlbum(
                bearerToken = "Bearer $token",
                bucketId = bucketId,
                albumName = albumName
            ) { completed, total ->
                lastCompleted = completed
                lastTotal = total
                setForeground(createForegroundInfo(albumName, completed, total))
            }
            showCompletionNotification(albumName, result.successCount, result.failureCount)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val albumName = inputData.getString(KEY_ALBUM_NAME) ?: "相册"
        return createForegroundInfo(albumName, lastCompleted, lastTotal)
    }

    private fun createForegroundInfo(albumName: String, completed: Int, total: Int): ForegroundInfo {
        val notification = buildProgressNotification(albumName, completed, total)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else 0
        return ForegroundInfo(NOTIFICATION_ID, notification, type)
    }

    private suspend fun showCompletionNotification(albumName: String, success: Int, failed: Int) {
        if (!hasPostNotificationPermission()) return
        val manager = NotificationManagerCompat.from(applicationContext)
        withContext(Dispatchers.Main) {
            val notification = buildCompletedNotification(albumName, success, failed)
            try {
                manager.notify(NOTIFICATION_ID + 1, notification)
            } catch (security: SecurityException) {
                // 权限在运行期被撤销，忽略即可
            }
        }
    }

    private fun buildProgressNotification(albumName: String, completed: Int, total: Int): Notification {
        ensureChannel()
        val contentText = if (total > 0) {
            applicationContext.getString(R.string.photo_backup_notification_progress, completed, total)
        } else {
            applicationContext.getString(R.string.photo_backup_notification_scanning)
        }
        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_photo_backup)
            .setContentTitle(applicationContext.getString(R.string.photo_backup_notification_title, albumName))
            .setContentText(contentText)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(if (total > 0) total else 0, if (total > 0) completed.coerceAtMost(total) else 0, total == 0)
            .build()
    }

    private fun buildCompletedNotification(albumName: String, success: Int, failed: Int): Notification {
        ensureChannel()
        val content = if (failed == 0) {
            applicationContext.getString(R.string.photo_backup_completed_success, success)
        } else {
            applicationContext.getString(R.string.photo_backup_completed_partial, success, failed)
        }
        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_photo_backup)
            .setContentTitle(applicationContext.getString(R.string.photo_backup_completed_title, albumName))
            .setContentText(content)
            .setAutoCancel(true)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                )
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun hasPostNotificationPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            applicationContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @AssistedFactory
    interface Factory : WorkerAssistedFactory<PhotoBackupWorker> {
        override fun create(appContext: Context, params: WorkerParameters): PhotoBackupWorker
    }
}
