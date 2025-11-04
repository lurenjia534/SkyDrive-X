package com.lurenjia534.skydrivex.data.local.media

import android.net.Uri

data class LocalAlbum(
    val bucketId: String,
    val displayName: String,
    val coverUri: Uri?,
    val itemCount: Int,
    val isBackedUp: Boolean = false
)
