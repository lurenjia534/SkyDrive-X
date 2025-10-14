package com.lurenjia534.skydrivex.ui.notification

import android.app.DownloadManager
import android.content.Context
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object DownloadRegistry {
    private val customCancelFlags = ConcurrentHashMap<Int, AtomicBoolean>()
    private val dmIds = ConcurrentHashMap<Int, Long>()

    fun registerCustom(notificationId: Int, cancelFlag: AtomicBoolean) {
        customCancelFlags[notificationId] = cancelFlag
    }

    fun registerDownloadManager(notificationId: Int, dmId: Long) {
        dmIds[notificationId] = dmId
    }

    fun getCancelFlag(notificationId: Int): AtomicBoolean? = customCancelFlags[notificationId]

    fun cancel(context: Context, notificationId: Int) {
        // cancel custom job if exists
        customCancelFlags[notificationId]?.set(true)
        // cancel download manager task if exists
        dmIds[notificationId]?.let { id ->
            val dm = context.getSystemService(DownloadManager::class.java)
            try { dm?.remove(id) } catch (_: Exception) {}
        }
        DownloadProgressMonitor.stop(notificationId)
        TransferTracker.markCancelled(notificationId, "已取消")
        cleanup(notificationId)
    }

    fun cleanup(notificationId: Int) {
        customCancelFlags.remove(notificationId)
        dmIds.remove(notificationId)
        DownloadProgressMonitor.stop(notificationId)
    }
}
