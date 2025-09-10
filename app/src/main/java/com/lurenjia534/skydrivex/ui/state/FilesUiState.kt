package com.lurenjia534.skydrivex.ui.state

import com.lurenjia534.skydrivex.data.model.driveitem.DriveItemDto

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
    // Server-side search state
    val searchResults: List<DriveItemDto>? = null,
    val isSearching: Boolean = false,
    val searchError: String? = null,
)
