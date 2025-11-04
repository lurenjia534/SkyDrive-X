package com.lurenjia534.skydrivex.data.local.photosync

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_uploads")
data class MediaUploadEntity(
    @PrimaryKey val contentUri: String,
    val bucketId: String,
    val displayName: String?,
    val size: Long,
    val dateModified: Long,
    val remoteItemId: String?,
    val status: UploadStatus,
    val lastAttemptAt: Long
)
