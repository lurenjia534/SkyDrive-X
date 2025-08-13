package com.lurenjia534.skydrivex.viewmodel

import com.lurenjia534.skydrivex.data.model.drive.DriveDto

/**
 * UI state for drive quota.
 *
 * @property data drive quota data or null when not loaded
 * @property isLoading true while loading drive data
 * @property error error message when loading fails
 */
data class DriveUiState(
    val data: DriveDto?,
    val isLoading: Boolean,
    val error: String?,
)
