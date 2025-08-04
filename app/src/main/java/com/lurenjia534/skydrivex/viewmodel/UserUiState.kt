package com.lurenjia534.skydrivex.viewmodel

import com.lurenjia534.skydrivex.data.model.UserDto

/**
 * UI state for user information screen.
 *
 * @property data user profile data or null when not loaded
 * @property isLoading true while loading user data
 * @property error error message when loading fails
 */
data class UserUiState(
    val data: UserDto?,
    val isLoading: Boolean,
    val error: String?
)

