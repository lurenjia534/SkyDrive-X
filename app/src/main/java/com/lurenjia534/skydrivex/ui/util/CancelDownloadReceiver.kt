package com.lurenjia534.skydrivex.ui.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CancelDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val nid = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (nid != -1) {
            DownloadRegistry.cancel(context, nid)
            // Replace progress with a fresh 'cancelled' notification to avoid stuck-progress UI
            replaceWithCompletion(context, nid, intent.getStringExtra(EXTRA_TITLE) ?: "下载", success = false, message = "已取消")
            DownloadRegistry.cleanup(nid)
        }
    }

    companion object {
        const val ACTION_CANCEL = "com.lurenjia534.skydrivex.action.CANCEL_DOWNLOAD"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_TITLE = "title"
    }
}

