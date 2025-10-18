package com.lurenjia534.skydrivex.ui.notification

import android.content.Context
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import com.lurenjia534.skydrivex.ui.notification.TransferTracker.Status

private const val TAG_UPLOAD_NOTIF = "UploadNotif"

/** Upload-related notification helpers. */
fun beginIndeterminateUpload(context: Context, title: String): Pair<Int, AtomicBoolean> {
    createDownloadChannel(context)
    val id = nextNotificationId()
    val cancel = AtomicBoolean(false)
    Log.d(TAG_UPLOAD_NOTIF, "beginIndeterminateUpload id=$id title=$title")
    showOrUpdateProgress(
        context = context,
        notificationId = id,
        title = title,
        progress = null,
        max = null,
        indeterminate = true,
        withCancelAction = true,
        smallIconRes = android.R.drawable.stat_sys_upload
    )
    DownloadRegistry.registerCustom(id, cancel)
    TransferTracker.start(
        notificationId = id,
        title = title,
        type = TransferTracker.TransferType.UPLOAD,
        allowCancel = true,
        indeterminate = true
    )
    return id to cancel
}

fun beginProgressUpload(context: Context, title: String, initialPercent: Int = 0): Pair<Int, AtomicBoolean> {
    createDownloadChannel(context)
    val id = nextNotificationId()
    val cancel = AtomicBoolean(false)
    Log.d(TAG_UPLOAD_NOTIF, "beginProgressUpload id=$id title=$title initial=$initialPercent")
    showOrUpdateProgress(
        context = context,
        notificationId = id,
        title = title,
        progress = initialPercent,
        max = 100,
        indeterminate = false,
        withCancelAction = true,
        smallIconRes = android.R.drawable.stat_sys_upload
    )
    DownloadRegistry.registerCustom(id, cancel)
    TransferTracker.start(
        notificationId = id,
        title = title,
        type = TransferTracker.TransferType.UPLOAD,
        allowCancel = true,
        indeterminate = false
    )
    TransferTracker.updateProgress(id, initialPercent, indeterminate = false)
    return id to cancel
}

fun updateUploadProgress(context: Context, id: Int, title: String, uploaded: Long, total: Long) {
    val pct = if (total > 0) ((uploaded * 100.0) / total).toInt().coerceIn(0, 100) else 0
    Log.v(TAG_UPLOAD_NOTIF, "updateUploadProgress id=$id title=$title uploaded=$uploaded total=$total pct=$pct")
    showOrUpdateProgress(
        context = context,
        notificationId = id,
        title = title,
        progress = pct,
        max = 100,
        indeterminate = false,
        withCancelAction = true,
        smallIconRes = android.R.drawable.stat_sys_upload
    )
    TransferTracker.updateProgress(id, pct, indeterminate = false)
}

fun finishUpload(
    context: Context,
    id: Int,
    title: String,
    status: Status,
    message: String? = null
) {
    val defaultMsg = when (status) {
        Status.SUCCESS -> "上传完成"
        Status.CANCELLED -> "已取消"
        else -> "上传失败"
    }
    val msg = message ?: defaultMsg
    val success = status == Status.SUCCESS
    Log.d(TAG_UPLOAD_NOTIF, "finishUpload id=$id title=$title status=$status msg=$msg")
    when (status) {
        Status.SUCCESS -> TransferTracker.markSuccess(id, msg)
        Status.CANCELLED -> TransferTracker.markCancelled(id, msg)
        Status.FAILED -> TransferTracker.markFailed(id, msg)
        Status.RUNNING -> TransferTracker.updateProgress(id, progress = null, indeterminate = true)
    }
    replaceWithCompletion(context, id, title, success, msg)
    DownloadRegistry.cleanup(id)
}
