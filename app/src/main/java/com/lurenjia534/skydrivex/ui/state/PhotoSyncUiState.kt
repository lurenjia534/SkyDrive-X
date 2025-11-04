package com.lurenjia534.skydrivex.ui.state

import com.lurenjia534.skydrivex.data.local.media.LocalAlbum

data class PhotoSyncUiState(
    val permissionGranted: Boolean = false,
    val isLoading: Boolean = false,
    val albums: List<LocalAlbum>? = null,
    val error: String? = null
)
