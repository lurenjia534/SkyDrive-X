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
 * @property canGoBack true when there is a parent breadcrumb to navigate to
 * @property path current breadcrumb trail from root to the visible folder
 * @property selectionMode true when multi-select mode is active
 * @property selectedIds currently selected item ids when in selection mode
 * @property searchResults optional list returned by remote search
 * @property isSearching true while search request is in-flight
 * @property searchError error message when search fails
 */
data class FilesUiState(
    val items: List<DriveItemDto>?,
    val isLoading: Boolean,
    val error: String?,
    val canGoBack: Boolean,
    val path: List<Breadcrumb>,
    val selectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    // Server-side search state
    val searchResults: List<DriveItemDto>? = null,
    val isSearching: Boolean = false,
    val searchError: String? = null,
)
