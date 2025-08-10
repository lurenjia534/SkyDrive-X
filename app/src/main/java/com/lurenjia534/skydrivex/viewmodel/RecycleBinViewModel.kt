package com.lurenjia534.skydrivex.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurenjia534.skydrivex.auth.AuthManager
import com.lurenjia534.skydrivex.data.model.RecycleBinItemDto
import com.lurenjia534.skydrivex.data.repository.RecycleBinRepository
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RecycleBinUiState {
    data class Success(val items: List<RecycleBinItemDto>) : RecycleBinUiState
    data class Error(val message: String) : RecycleBinUiState
    object Loading : RecycleBinUiState
    object Idle : RecycleBinUiState
}

@HiltViewModel
class RecycleBinViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val recycleBinRepository: RecycleBinRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecycleBinUiState>(RecycleBinUiState.Idle)
    val uiState: StateFlow<RecycleBinUiState> = _uiState.asStateFlow()

    private val scopes = arrayOf("Files.ReadWrite")

    private fun <T> withToken(
        onSuccess: suspend (token: String) -> T,
        onError: (error: String) -> Unit
    ) {
        viewModelScope.launch {
            if (authManager.awaitInitialization()) {
                authManager.getCurrentAccount(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
                    override fun onAccountLoaded(activeAccount: IAccount?) {
                        if (activeAccount != null) {
                            authManager.acquireTokenSilent(activeAccount, scopes, object : SilentAuthenticationCallback {
                                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                                    viewModelScope.launch {
                                        try {
                                            onSuccess("Bearer ${authenticationResult.accessToken}")
                                        } catch (e: Exception) {
                                            onError(e.message ?: "Operation failed.")
                                        }
                                    }
                                }

                                override fun onError(exception: MsalException) {
                                    onError(exception.message ?: "Failed to acquire token.")
                                }
                            })
                        } else {
                            onError("No signed in account.")
                        }
                    }

                    override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                        // If account changes, the UI should probably be refreshed.
                        // For simplicity, we'll just re-fetch.
                        if (currentAccount != null) {
                            fetchRecycleBinItems()
                        } else {
                            _uiState.value = RecycleBinUiState.Error("No signed in account.")
                        }
                    }

                    override fun onError(exception: MsalException) {
                        onError(exception.message ?: "Failed to get account.")
                    }
                })
            } else {
                onError("MSAL not initialized.")
            }
        }
    }

    fun fetchRecycleBinItems() {
        _uiState.value = RecycleBinUiState.Loading
        withToken(
            onSuccess = { token ->
                val items = recycleBinRepository.getRecycleBinItems(token)
                _uiState.value = RecycleBinUiState.Success(items)
            },
            onError = { error ->
                _uiState.value = RecycleBinUiState.Error(error)
            }
        )
    }

    fun restoreItem(itemId: String) {
        withToken(
            onSuccess = { token ->
                recycleBinRepository.restoreItem(token, itemId)
                fetchRecycleBinItems() // Refresh list
            },
            onError = { error ->
                // In a real app, this should be shown to the user (e.g., via a Snackbar)
                println("Error restoring item: $error")
            }
        )
    }

    fun deleteItem(itemId: String) {
        withToken(
            onSuccess = { token ->
                recycleBinRepository.deleteItem(token, itemId)
                fetchRecycleBinItems() // Refresh list
            },
            onError = { error ->
                println("Error deleting item: $error")
            }
        )
    }
}
