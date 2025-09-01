package com.lurenjia534.skydrivex.ui.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.IntentCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.lurenjia534.skydrivex.ui.notification.DownloadRegistry
import com.lurenjia534.skydrivex.ui.notification.createDownloadChannel
import com.lurenjia534.skydrivex.ui.notification.finishUpload
import com.lurenjia534.skydrivex.ui.notification.updateUploadProgress
import com.lurenjia534.skydrivex.ui.notification.CancelDownloadReceiver
import com.lurenjia534.skydrivex.data.repository.FilesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 *用于用户启动的上传的前景服务。
 *使用ForegroundServiceType = DataSync符合Android 14+。
 */
@AndroidEntryPoint
class TransferService : LifecycleService() {

    @Inject lateinit var filesRepository: FilesRepository

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent == null) return START_NOT_STICKY
        val action = intent.action
        when (action) {
            ACTION_UPLOAD_SMALL -> handleSmallUpload(intent, startId)
            ACTION_UPLOAD_LARGE -> handleLargeUpload(intent, startId)
            else -> stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    private fun sendUploadCompletedBroadcast(parentId: String?) {
        val intent = Intent(ACTION_UPLOAD_COMPLETED).apply {
            setPackage(packageName)
            putExtra(EXTRA_PARENT_ID, parentId ?: "root")
        }
        runCatching { sendBroadcast(intent) }
    }

    private fun handleSmallUpload(intent: Intent, startId: Int) {
        val token = intent.getStringExtra(EXTRA_TOKEN) ?: return stopSelf(startId)
        val parentId = intent.getStringExtra(EXTRA_PARENT_ID)
        val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: return stopSelf(startId)
        val mime = intent.getStringExtra(EXTRA_MIME) ?: "application/octet-stream"
        val uri: Uri = IntentCompat.getParcelableExtra(intent, EXTRA_URI, Uri::class.java) ?: return stopSelf(startId)

        val (nid, cancelFlag) = startAsForeground("正在上传: $fileName")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (cancelFlag.get()) throw CancellationException("Cancelled")
                val bytes = contentResolver.openInputStream(uri)?.use(InputStream::readBytes) ?: ByteArray(0)
                if (bytes.isEmpty()) error("读取文件失败")
                filesRepository.uploadSmallFile(
                    parentId = parentId,
                    token = "Bearer $token",
                    fileName = fileName,
                    mimeType = mime,
                    bytes = bytes
                )
                finishUpload(this@TransferService, nid, fileName, success = true)
                sendUploadCompletedBroadcast(parentId)
            } catch (e: Exception) {
                val cancelled = cancelFlag.get() || e is CancellationException
                finishUpload(this@TransferService, nid, fileName, success = false, message = if (cancelled) "已取消" else e.message)
                Log.e(TAG, "Small upload failed: $fileName", e)
            } finally {
                stopSelf(startId)
            }
        }
    }

    private fun handleLargeUpload(intent: Intent, startId: Int) {
        val token = intent.getStringExtra(EXTRA_TOKEN) ?: return stopSelf(startId)
        val parentId = intent.getStringExtra(EXTRA_PARENT_ID)
        val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: return stopSelf(startId)
        val totalBytes = intent.getLongExtra(EXTRA_TOTAL_BYTES, -1L)
        val uri: Uri = IntentCompat.getParcelableExtra(intent, EXTRA_URI, Uri::class.java)
            ?: return stopSelf(startId)

        val (nid, cancelFlag) = startAsForeground("正在上传: $fileName")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (totalBytes <= 0) {
                    // fallback to small upload path if size unknown
                    val bytes = contentResolver.openInputStream(uri)?.use(InputStream::readBytes) ?: ByteArray(0)
                    if (bytes.isEmpty()) error("读取文件失败")
                    filesRepository.uploadSmallFile(
                        parentId = parentId,
                        token = "Bearer $token",
                        fileName = fileName,
                        mimeType = guessMime(fileName),
                        bytes = bytes
                    )
                    finishUpload(this@TransferService, nid, fileName, success = true)
                    sendUploadCompletedBroadcast(parentId)
                } else {
                    val item = filesRepository.uploadLargeFile(
                        parentId = parentId,
                        token = "Bearer $token",
                        fileName = fileName,
                        totalBytes = totalBytes,
                        bytesProvider = { offset, wantSize ->
                            withContext(Dispatchers.IO) {
                                contentResolver.openInputStream(uri)?.use { ins ->
                                    skipTo(ins, offset)
                                    val buf = ByteArray(wantSize)
                                    var readTotal = 0
                                    while (readTotal < wantSize) {
                                        val r = ins.read(buf, readTotal, wantSize - readTotal)
                                        if (r <= 0) break
                                        readTotal += r
                                    }
                                    if (readTotal == wantSize) buf else buf.copyOf(readTotal)
                                } ?: ByteArray(0)
                            }
                        },
                        cancelFlag = cancelFlag,
                        onProgress = { uploaded, total ->
                            updateUploadProgress(this@TransferService, nid, fileName, uploaded, total)
                        }
                    )
                    finishUpload(this@TransferService, nid, item.name ?: fileName, success = true)
                    sendUploadCompletedBroadcast(parentId)
                }
            } catch (e: Exception) {
                val cancelled = cancelFlag.get() || e is CancellationException
                finishUpload(this@TransferService, nid, fileName, success = false, message = if (cancelled) "已取消" else e.message)
                Log.e(TAG, "Large upload failed: $fileName", e)
            } finally {
                stopSelf(startId)
            }
        }
    }

    private fun skipTo(ins: InputStream, target: Long) {
        var skipped = 0L
        while (skipped < target) {
            val s = ins.skip(target - skipped)
            if (s <= 0) break
            skipped += s
        }
    }

    private fun guessMime(name: String): String = when {
        name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) -> "image/jpeg"
        name.endsWith(".png", true) -> "image/png"
        name.endsWith(".gif", true) -> "image/gif"
        else -> "application/octet-stream"
    }

    private fun startAsForeground(title: String): Pair<Int, AtomicBoolean> {
        createDownloadChannel(this)
        val notificationId = ((System.currentTimeMillis() % Int.MAX_VALUE)).toInt()
        val cancelFlag = AtomicBoolean(false)
        val builder = NotificationCompat.Builder(this, "downloads")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(title)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(0, 0, true)

            val cancelIntent = Intent(this, CancelDownloadReceiver::class.java).apply {
                action = CancelDownloadReceiver.ACTION_CANCEL
                putExtra(CancelDownloadReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(CancelDownloadReceiver.EXTRA_TITLE, title)
            }
            val pi = PendingIntent.getBroadcast(
                this,
                notificationId,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "取消", pi)

        val notification: Notification = builder.build()
        // Register cancel flag so CancelDownloadReceiver can cancel upload
        DownloadRegistry.registerCustom(notificationId, cancelFlag)
        // Start foreground with typed API on Android 14+; older versions use 2-arg
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(notificationId, notification)
        }
        return notificationId to cancelFlag
    }

    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)

    companion object {
        private const val TAG = "TransferService"
        const val ACTION_UPLOAD_SMALL = "com.lurenjia534.skydrivex.action.UPLOAD_SMALL"
        const val ACTION_UPLOAD_LARGE = "com.lurenjia534.skydrivex.action.UPLOAD_LARGE"
        const val ACTION_UPLOAD_COMPLETED = "com.lurenjia534.skydrivex.action.UPLOAD_COMPLETED"

        const val EXTRA_TOKEN = "extra_token"
        const val EXTRA_PARENT_ID = "extra_parent_id"
        const val EXTRA_FILE_NAME = "extra_file_name"
        const val EXTRA_MIME = "extra_mime"
        const val EXTRA_URI = "extra_uri"
        const val EXTRA_TOTAL_BYTES = "extra_total_bytes"
    }
}

// No compat helper needed; we gate the typed call by Build.VERSION.
