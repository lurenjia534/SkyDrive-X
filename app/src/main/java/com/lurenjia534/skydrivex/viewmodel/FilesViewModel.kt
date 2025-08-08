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

    private val _filesState = MutableStateFlow(
        FilesUiState(items = null, isLoading = false, error = null, canGoBack = false)
    )
    val filesState: StateFlow<FilesUiState> = _filesState.asStateFlow()

    private val cache = mutableMapOf<String, List<DriveItemDto>>()
    private val stack = mutableListOf<String>()

    fun loadRoot(token: String) {
        stack.clear()
        stack.add("root")
        load("root") { filesRepository.getRootChildren("Bearer $token") }
    }

    fun loadChildren(itemId: String, token: String) {
        stack.add(itemId)
        load(itemId) { filesRepository.getChildren(itemId, "Bearer $token") }
    }

    private fun load(key: String, request: suspend () -> List<DriveItemDto>) {
        cache[key]?.let {
            _filesState.value = FilesUiState(
                items = it,
                isLoading = false,
                error = null,
                canGoBack = stack.size > 1
            )
            return
        }
        viewModelScope.launch {
            _filesState.value = FilesUiState(
                items = null,
                isLoading = true,
                error = null,
                canGoBack = stack.size > 1
            )
            try {
                val items = request()
                cache[key] = items
                _filesState.value = FilesUiState(
                    items = items,
                    isLoading = false,
                    error = null,
                    canGoBack = stack.size > 1
                )
            } catch (e: Exception) {
                // 回退栈（如果此次加载是深入子层级失败）
                if (stack.isNotEmpty() && stack.last() == key && stack.size > 1) {
                    stack.removeAt(stack.lastIndex)
                }
                _filesState.value = FilesUiState(
                    items = null,
                    isLoading = false,
                    error = e.message,
                    canGoBack = stack.size > 1
                )
            }
        }
    }

    fun goBack(token: String) {
        if (stack.size <= 1) return
        // 弹出当前层级（避免使用 API 35 的 List#removeLast）
        stack.removeAt(stack.lastIndex)
        val key = stack.last()
        cache[key]?.let { cached ->
            _filesState.value = FilesUiState(
                items = cached,
                isLoading = false,
                error = null,
                canGoBack = stack.size > 1
            )
            return
        }
        viewModelScope.launch {
            _filesState.value = FilesUiState(
                items = null,
                isLoading = true,
                error = null,
                canGoBack = stack.size > 1
            )
            try {
                val items = if (key == "root") {
                    filesRepository.getRootChildren("Bearer $token")
                } else {
                    filesRepository.getChildren(key, "Bearer $token")
                }
                cache[key] = items
                _filesState.value = FilesUiState(
                    items = items,
                    isLoading = false,
                    error = null,
                    canGoBack = stack.size > 1
                )
            } catch (e: Exception) {
                _filesState.value = FilesUiState(
                    items = null,
                    isLoading = false,
                    error = e.message,
                    canGoBack = stack.size > 1
                )
            }
        }
    }
}
