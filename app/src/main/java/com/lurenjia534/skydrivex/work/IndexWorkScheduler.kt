package com.lurenjia534.skydrivex.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import com.lurenjia534.skydrivex.data.local.index.IndexPreferenceRepository

@Singleton
class IndexWorkScheduler @Inject constructor(
    private val prefs: IndexPreferenceRepository
) {

    suspend fun ensurePeriodic(context: Context) {
        val snapshot = prefs.snapshot.first()
        if (!snapshot.enabled) {
            cancelAll(context)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (snapshot.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(false)
            .setRequiresCharging(snapshot.chargeOnly)
            .build()

        val request = PeriodicWorkRequestBuilder<IndexSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            IndexSyncWorker.UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(IndexSyncWorker.UNIQUE_PERIODIC)
        WorkManager.getInstance(context).cancelUniqueWork(IndexSyncWorker.UNIQUE_REBUILD)
    }

    suspend fun enqueueRebuild(context: Context) {
        val snapshot = prefs.snapshot.first()
        if (!snapshot.enabled) return

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (snapshot.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresCharging(snapshot.chargeOnly)
            .build()

        val request = OneTimeWorkRequestBuilder<IndexSyncWorker>()
            .setConstraints(constraints)
            .setInputData(IndexSyncWorker.inputData(rebuild = true))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IndexSyncWorker.UNIQUE_REBUILD,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}

