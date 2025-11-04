package com.lurenjia534.skydrivex.ui.viewmodel.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurenjia534.skydrivex.data.repository.FilesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ImagePreviewViewModel @Inject constructor(
    private val filesRepository: FilesRepository,
) : ViewModel() {

    private val _imageUrls = MutableStateFlow<Map<String, String?>>(emptyMap())
    val imageUrls: StateFlow<Map<String, String?>> = _imageUrls.asStateFlow()

    private val _loadingIds = MutableStateFlow<Set<String>>(emptySet())
    val loadingIds: StateFlow<Set<String>> = _loadingIds.asStateFlow()

    private val _errors = MutableStateFlow<Map<String, String>>(emptyMap())
    val errors: StateFlow<Map<String, String>> = _errors.asStateFlow()

    fun ensureLoaded(itemId: String, token: String) {
        if (_imageUrls.value.containsKey(itemId) || _loadingIds.value.contains(itemId)) return
        viewModelScope.launch {
            _loadingIds.update { it + itemId }
            _errors.update { it - itemId }
            try {
                val url = filesRepository.getDownloadUrl(itemId, "Bearer $token")
                _imageUrls.update { it + (itemId to url) }
            } catch (e: Exception) {
                _errors.update { it + (itemId to (e.message ?: "加载失败")) }
            } finally {
                _loadingIds.update { it - itemId }
            }
        }
    }

    fun clearError(itemId: String) {
        _errors.update { it - itemId }
    }

    fun resetCache() {
        _imageUrls.value = emptyMap()
        _loadingIds.value = emptySet()
        _errors.value = emptyMap()
    }
}
