package com.lurenjia534.skydrivex.data.local.db

import com.lurenjia534.skydrivex.data.local.db.mapper.toEntity
import com.lurenjia534.skydrivex.data.local.db.mapper.toTrackerEntry
import com.lurenjia534.skydrivex.ui.notification.TransferTracker
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class TransferRepository @Inject constructor(
    private val localDataSource: TransferLocalDataSource
) {
    fun observeTransfers(): Flow<List<TransferTracker.Entry>> =
        localDataSource.observeTransfers().map { entities ->
            entities.map { it.toTrackerEntry() }
        }

    suspend fun upsert(entry: TransferTracker.Entry) = localDataSource.upsert(entry.toEntity())

    suspend fun updateStatus(
        notificationId: Int,
        status: TransferTracker.Status,
        progress: Int?,
        indeterminate: Boolean,
        message: String?,
        completedAt: Long?
    ) = localDataSource.updateStatus(
        notificationId = notificationId,
        status = status,
        progress = progress,
        indeterminate = indeterminate,
        message = message,
        completedAt = completedAt
    )

    suspend fun remove(notificationId: Int) = localDataSource.remove(notificationId)

    suspend fun clearFinished() = localDataSource.clearFinished()
}
