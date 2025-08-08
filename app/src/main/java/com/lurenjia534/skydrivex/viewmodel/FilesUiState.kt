package com.lurenjia534.skydrivex.viewmodel

import com.lurenjia534.skydrivex.data.model.DriveItemDto

data class Breadcrumb(
    val id: String,
    val name: String,
)

/**
 * UI state for file listings.
 *
 * @property items list of drive items or null when not loaded
 * @property isLoading true while loading file data
 * @property error error message when loading fails
 */
data class FilesUiState(
    val items: List<DriveItemDto>?,
    val isLoading: Boolean,
    val error: String?,
    val canGoBack: Boolean,
    val path: List<Breadcrumb>,
)
