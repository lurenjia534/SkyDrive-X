package com.lurenjia534.skydrivex

import android.app.Application
import com.lurenjia534.skydrivex.ui.notification.TransferTracker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SkyDriveXApp : Application() {

    @Inject lateinit var transferTracker: TransferTracker

    override fun onCreate() {
        super.onCreate()
        // 强制注入以触发 TransferTracker 初始化
        transferTracker
    }
}
