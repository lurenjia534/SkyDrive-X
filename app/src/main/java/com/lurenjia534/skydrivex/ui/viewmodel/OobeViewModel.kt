package com.lurenjia534.skydrivex.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurenjia534.skydrivex.data.repository.AuthConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OobeUiState(
    val clientId: String = "",
    val clientIdError: String? = null,
    val isSaving: Boolean = false,
    val hasExistingConfig: Boolean = false
)

sealed interface OobeEvent {
    data object Completed : OobeEvent
}

@HiltViewModel
class OobeViewModel @Inject constructor(
    private val authConfigRepository: AuthConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OobeUiState())
    val uiState: StateFlow<OobeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<OobeEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            authConfigRepository.getConfig()?.let { config ->
                _uiState.update {
                    it.copy(
                        clientId = config.clientId,
                        hasExistingConfig = true
                    )
                }
            }
        }
    }

    fun onClientIdChanged(value: String) {
        _uiState.update { it.copy(clientId = value, clientIdError = null) }
    }

    fun submit() {
        val trimmed = _uiState.value.clientId.trim()
        if (!isValidClientId(trimmed)) {
            _uiState.update { it.copy(clientIdError = "请输入有效的 Client ID（GUID）") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, clientIdError = null) }
            authConfigRepository.saveClientId(trimmed)
            _uiState.update { it.copy(isSaving = false, hasExistingConfig = true) }
            _events.emit(OobeEvent.Completed)
        }
    }

    private fun isValidClientId(input: String): Boolean = runCatching {
        UUID.fromString(input)
    }.isSuccess
}
