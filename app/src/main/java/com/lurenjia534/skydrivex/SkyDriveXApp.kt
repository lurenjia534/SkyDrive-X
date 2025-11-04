package com.lurenjia534.skydrivex

import android.app.Application
import androidx.work.Configuration
import androidx.hilt.work.HiltWorkerFactory
import com.lurenjia534.skydrivex.ui.notification.TransferTracker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SkyDriveXApp : Application(), Configuration.Provider {

    @Inject lateinit var transferTracker: TransferTracker
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        // 强制注入以触发 TransferTracker 初始化
        transferTracker
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .setWorkerFactory(workerFactory)
            .build()
}
