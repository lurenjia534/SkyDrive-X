package com.lurenjia534.skydrivex.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoBackupScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun enqueueAlbum(bucketId: String, albumName: String) {
        val request = OneTimeWorkRequestBuilder<PhotoBackupWorker>()
            .setInputData(
                workDataOf(
                    PhotoBackupWorker.KEY_BUCKET_ID to bucketId,
                    PhotoBackupWorker.KEY_ALBUM_NAME to albumName
                )
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName(bucketId),
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun cancelAlbum(bucketId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(bucketId))
    }

    private fun uniqueWorkName(bucketId: String): String = "photo-backup-$bucketId"
}
