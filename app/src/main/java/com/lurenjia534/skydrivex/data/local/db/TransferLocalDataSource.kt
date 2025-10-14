package com.lurenjia534.skydrivex.data.local.db

import com.lurenjia534.skydrivex.data.local.db.TransferEntity
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

        // 如果记录已经终结，再收到 RUNNING 状态的进度更新则忽略，避免覆盖完成/失败状态
        if (existing != null && existing.status != TransferStatus.RUNNING && status == TransferTracker.Status.RUNNING) {
            return@withContext
        }

        val base = existing ?: TransferEntity(
            notificationId = notificationId,
            title = "",
            type = TransferType.DOWNLOAD_CUSTOM,
            status = TransferStatus.RUNNING,
            progress = null,
            indeterminate = true,
            allowCancel = false,
            message = null,
            startedAt = System.currentTimeMillis(),
            completedAt = null
        )

        val resolvedStatus = status.toTransferStatus()
        val resolvedProgress = progress ?: base.progress
        val resolvedMessage = message ?: if (status == TransferTracker.Status.SUCCESS) "完成" else base.message
        val resolvedCompletedAt = when (status) {
            TransferTracker.Status.RUNNING -> base.completedAt
            else -> completedAt ?: base.completedAt ?: System.currentTimeMillis()
        }

        val updated = base.copy(
            status = resolvedStatus,
            progress = resolvedProgress,
            indeterminate = if (status == TransferTracker.Status.RUNNING) indeterminate else false,
            message = resolvedMessage,
            allowCancel = if (status == TransferTracker.Status.RUNNING) base.allowCancel else false,
            completedAt = resolvedCompletedAt
        )
        dao.upsert(updated)
    }

    suspend fun clearFinished() = withContext(ioDispatcher) {
        dao.clearFinished(TransferStatus.RUNNING)
    }
}
