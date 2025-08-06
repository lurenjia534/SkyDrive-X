package com.lurenjia534.skydrivex.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurenjia534.skydrivex.data.model.DriveItemDto
import com.lurenjia534.skydrivex.data.repository.FilesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FilesViewModel @Inject constructor(
    private val filesRepository: FilesRepository,
) : ViewModel() {

    private val _filesState = MutableStateFlow(FilesUiState(items = null, isLoading = false, error = null))
    val filesState: StateFlow<FilesUiState> = _filesState.asStateFlow()

    private val cache = mutableMapOf<String, List<DriveItemDto>>()

    fun loadRoot(token: String) {
        load("root") { filesRepository.getRootChildren("Bearer $token") }
    }

    fun loadChildren(itemId: String, token: String) {
        load(itemId) { filesRepository.getChildren(itemId, "Bearer $token") }
    }

    private fun load(key: String, request: suspend () -> List<DriveItemDto>) {
        cache[key]?.let {
            _filesState.value = FilesUiState(items = it, isLoading = false, error = null)
            return
        }
        viewModelScope.launch {
            _filesState.value = FilesUiState(items = null, isLoading = true, error = null)
            try {
                val items = request()
                cache[key] = items
                _filesState.value = FilesUiState(items = items, isLoading = false, error = null)
            } catch (e: Exception) {
                _filesState.value = FilesUiState(items = null, isLoading = false, error = e.message)
            }
        }
    }
}
