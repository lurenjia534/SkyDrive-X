package com.lurenjia534.skydrivex.data.local.db

import com.lurenjia534.skydrivex.data.local.db.dao.TransferDao
import com.lurenjia534.skydrivex.data.local.db.mapper.toTransferStatus
import com.lurenjia534.skydrivex.data.local.db.model.TransferStatus
import com.lurenjia534.skydrivex.data.local.db.model.TransferType
import com.lurenjia534.skydrivex.ui.notification.TransferTracker
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Singleton
class TransferLocalDataSource @Inject constructor(
    private val dao: TransferDao,
    private val ioDispatcher: CoroutineDispatcher
) {
    fun observeTransfers(): Flow<List<TransferEntity>> =
        dao.observeTransfers()

    suspend fun upsert(entity: TransferEntity) = withContext(ioDispatcher) {
        dao.upsert(entity)
    }

    suspend fun remove(notificationId: Int) = withContext(ioDispatcher) {
        dao.delete(notificationId)
    }

    suspend fun updateStatus(
        notificationId: Int,
        status: TransferTracker.Status,
        progress: Int?,
        indeterminate: Boolean,
        message: String?,
        completedAt: Long?
    ) = withContext(ioDispatcher) {
        val existing = dao.findById(notificationId)

        if (status == TransferTracker.Status.RUNNING) {
            val resolvedProgress = progress ?: existing?.progress
            val affected = dao.updateProgressIfRunning(
                notificationId = notificationId,
                progress = resolvedProgress,
                indeterminate = indeterminate,
                message = message
            )
            if (affected == 0 && existing == null) {
                dao.upsert(
                    TransferEntity(
                        notificationId = notificationId,
                        title = "",
                        type = TransferType.DOWNLOAD_CUSTOM,
                        status = TransferStatus.RUNNING,
                        progress = resolvedProgress,
                        indeterminate = indeterminate,
                        allowCancel = true,
                        message = message,
                        startedAt = System.currentTimeMillis(),
                        completedAt = null
                    )
                )
            }
            return@withContext
        }

        val base = existing ?: TransferEntity(
            notificationId = notificationId,
            title = "",
            type = TransferType.DOWNLOAD_CUSTOM,
            status = TransferStatus.RUNNING,
            progress = progress,
            indeterminate = indeterminate,
            allowCancel = true,
            message = message,
            startedAt = System.currentTimeMillis(),
            completedAt = null
        )

        val resolvedStatus = status.toTransferStatus()
        val resolvedProgress = progress ?: base.progress ?: if (status == TransferTracker.Status.SUCCESS) 100 else null
        val resolvedMessage = message ?: if (status == TransferTracker.Status.SUCCESS) "完成" else base.message
        val resolvedCompletedAt = completedAt ?: base.completedAt ?: System.currentTimeMillis()

        val updated = base.copy(
            status = resolvedStatus,
            progress = resolvedProgress,
            indeterminate = false,
            message = resolvedMessage,
            allowCancel = false,
            completedAt = resolvedCompletedAt
        )
        dao.upsert(updated)
    }

    suspend fun clearFinished() = withContext(ioDispatcher) {
        dao.clearFinished(TransferStatus.RUNNING)
    }
}
