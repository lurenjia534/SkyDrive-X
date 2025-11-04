package com.lurenjia534.skydrivex.data.local.media

import android.net.Uri

data class LocalMediaItem(
    val id: Long,
    val uri: Uri,
    val bucketId: String,
    val displayName: String?,
    val mimeType: String?,
    val isVideo: Boolean,
    val durationMillis: Long,
    val takenAtMillis: Long,
    val modifiedAtMillis: Long,
    val sizeBytes: Long
)
