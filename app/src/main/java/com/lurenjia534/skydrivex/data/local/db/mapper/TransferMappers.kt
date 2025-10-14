package com.lurenjia534.skydrivex.data.local.db.mapper

import com.lurenjia534.skydrivex.data.local.db.TransferEntity
import com.lurenjia534.skydrivex.data.local.db.model.TransferStatus
import com.lurenjia534.skydrivex.data.local.db.model.TransferType
import com.lurenjia534.skydrivex.ui.notification.TransferTracker

fun TransferEntity.toTrackerEntry(): TransferTracker.Entry = TransferTracker.Entry(
    notificationId = notificationId,
    title = title,
    type = type.toTrackerType(),
    status = status.toTrackerStatus(),
    progress = progress,
    indeterminate = indeterminate,
    allowCancel = allowCancel,
    message = message,
    startedAt = startedAt,
    completedAt = completedAt
)

fun TransferTracker.Entry.toEntity(): TransferEntity = TransferEntity(
    notificationId = notificationId,
    title = title,
    type = type.toTransferType(),
    status = status.toTransferStatus(),
    progress = progress,
    indeterminate = indeterminate,
    allowCancel = allowCancel,
    message = message,
    startedAt = startedAt,
    completedAt = completedAt
)

fun TransferTracker.Status.toTransferStatus(): TransferStatus = when (this) {
    TransferTracker.Status.RUNNING -> TransferStatus.RUNNING
    TransferTracker.Status.SUCCESS -> TransferStatus.SUCCESS
    TransferTracker.Status.FAILED -> TransferStatus.FAILED
    TransferTracker.Status.CANCELLED -> TransferStatus.CANCELLED
}

fun TransferTracker.TransferType.toTransferType(): TransferType = when (this) {
    TransferTracker.TransferType.DOWNLOAD_SYSTEM -> TransferType.DOWNLOAD_SYSTEM
    TransferTracker.TransferType.DOWNLOAD_CUSTOM -> TransferType.DOWNLOAD_CUSTOM
    TransferTracker.TransferType.UPLOAD -> TransferType.UPLOAD
}

private fun TransferType.toTrackerType(): TransferTracker.TransferType = when (this) {
    TransferType.DOWNLOAD_SYSTEM -> TransferTracker.TransferType.DOWNLOAD_SYSTEM
    TransferType.DOWNLOAD_CUSTOM -> TransferTracker.TransferType.DOWNLOAD_CUSTOM
    TransferType.UPLOAD -> TransferTracker.TransferType.UPLOAD
}

private fun TransferStatus.toTrackerStatus(): TransferTracker.Status = when (this) {
    TransferStatus.RUNNING -> TransferTracker.Status.RUNNING
    TransferStatus.SUCCESS -> TransferTracker.Status.SUCCESS
    TransferStatus.FAILED -> TransferTracker.Status.FAILED
    TransferStatus.CANCELLED -> TransferTracker.Status.CANCELLED
}
