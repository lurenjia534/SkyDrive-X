package com.lurenjia534.skydrivex.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lurenjia534.skydrivex.data.local.db.model.TransferStatus
import com.lurenjia534.skydrivex.data.local.db.model.TransferType

@Entity(
    tableName = "transfers",
    indices = [Index(value = ["started_at"], name = "idx_transfers_started_at")]
)
data class TransferEntity(
    @PrimaryKey @ColumnInfo(name = "notification_id") val notificationId: Int,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "type") val type: TransferType,
    @ColumnInfo(name = "status") val status: TransferStatus,
    @ColumnInfo(name = "progress") val progress: Int?,
    @ColumnInfo(name = "indeterminate") val indeterminate: Boolean,
    @ColumnInfo(name = "allow_cancel") val allowCancel: Boolean,
    @ColumnInfo(name = "message") val message: String?,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "completed_at") val completedAt: Long?
)
