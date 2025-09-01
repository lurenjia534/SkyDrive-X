package com.lurenjia534.skydrivex.ui.notification

import android.content.Context
import java.util.concurrent.atomic.AtomicBoolean

/** Upload-related notification helpers. */
fun beginIndeterminateUpload(context: Context, title: String): Pair<Int, AtomicBoolean> {
    createDownloadChannel(context)
    val id = nextNotificationId()
    val cancel = AtomicBoolean(false)
    showOrUpdateProgress(context, id, title, null, null, true, withCancelAction = true)
    DownloadRegistry.registerCustom(id, cancel)
    return id to cancel
}

fun beginProgressUpload(context: Context, title: String, initialPercent: Int = 0): Pair<Int, AtomicBoolean> {
    createDownloadChannel(context)
    val id = nextNotificationId()
    val cancel = AtomicBoolean(false)
    showOrUpdateProgress(context, id, title, initialPercent, 100, false, withCancelAction = true)
    DownloadRegistry.registerCustom(id, cancel)
    return id to cancel
}

fun updateUploadProgress(context: Context, id: Int, title: String, uploaded: Long, total: Long) {
    val pct = if (total > 0) ((uploaded * 100.0) / total).toInt().coerceIn(0, 100) else 0
    showOrUpdateProgress(context, id, title, pct, 100, false, withCancelAction = true)
}

fun finishUpload(context: Context, id: Int, title: String, success: Boolean, message: String? = null) {
    val msg = message ?: if (success) "上传完成" else "上传失败"
    replaceWithCompletion(context, id, title, success, msg)
    DownloadRegistry.cleanup(id)
}

