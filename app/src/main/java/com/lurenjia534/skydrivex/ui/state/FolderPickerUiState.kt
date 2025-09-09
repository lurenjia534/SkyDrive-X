package com.lurenjia534.skydrivex.ui.state

import com.lurenjia534.skydrivex.data.model.driveitem.DriveItemDto

data class FolderPickerUiState(
    val items: List<DriveItemDto>?,
    val isLoading: Boolean,
    val error: String?,
    val canGoBack: Boolean,
    val path: List<Breadcrumb>
)

