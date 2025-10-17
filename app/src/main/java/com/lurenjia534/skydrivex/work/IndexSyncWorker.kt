package com.lurenjia534.skydrivex.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.lurenjia534.skydrivex.data.local.index.IndexPreferenceRepository
import com.lurenjia534.skydrivex.data.repository.IndexRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

class IndexSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            IndexWorkerEntryPoint::class.java
        )
        val prefs = entryPoint.indexPrefs()
        val snapshot = prefs.snapshot.first()
        if (!snapshot.enabled) return Result.success()

        val rebuild = inputData.getBoolean(KEY_REBUILD, false)
        val repository = entryPoint.indexRepository()

        val result = repository.sync(rebuild)
        return result.fold(
            onSuccess = { Result.success() },
            onFailure = { error ->
                if (error is IllegalStateException && error.message?.contains("account") == true) {
                    Result.failure()
                } else Result.retry()
            }
        )
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface IndexWorkerEntryPoint {
        fun indexRepository(): IndexRepository
        fun indexPrefs(): IndexPreferenceRepository
    }

    companion object {
        const val UNIQUE_PERIODIC = "index_periodic_sync"
        const val UNIQUE_REBUILD = "index_rebuild"
        const val KEY_REBUILD = "rebuild"

        fun inputData(rebuild: Boolean) = workDataOf(KEY_REBUILD to rebuild)
    }
}
