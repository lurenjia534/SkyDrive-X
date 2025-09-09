package com.lurenjia534.skydrivex.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurenjia534.skydrivex.data.model.driveitem.DriveItemDto
import com.lurenjia534.skydrivex.data.repository.FilesRepository
import com.lurenjia534.skydrivex.ui.state.Breadcrumb
import com.lurenjia534.skydrivex.ui.state.FolderPickerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FolderPickerViewModel @Inject constructor(
    private val filesRepository: FilesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        FolderPickerUiState(items = null, isLoading = false, error = null, canGoBack = false, path = emptyList())
    )
    val state: StateFlow<FolderPickerUiState> = _state.asStateFlow()

    private val cache = mutableMapOf<String, List<DriveItemDto>>()
    private val stack = mutableListOf<Breadcrumb>()
    private var rootId: String? = null

    fun start(token: String) {
        // 若已初始化则不重复加载
        if (stack.isNotEmpty()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val rid = filesRepository.getRootId("Bearer $token")
                rootId = rid
                val rootName = "根目录"
                stack.clear()
                stack.add(Breadcrumb(id = rid ?: "root", name = rootName))
                val items = filesRepository.getRootChildren("Bearer $token").filter { it.folder != null }
                cache[stack.last().id] = items
                _state.value = FolderPickerUiState(
                    items = items,
                    isLoading = false,
                    error = null,
                    canGoBack = false,
                    path = stack.toList()
                )
            } catch (e: Exception) {
                _state.value = FolderPickerUiState(null, false, e.message, false, emptyList())
            }
        }
    }

    fun navigateInto(itemId: String, token: String, name: String) {
        stack.add(Breadcrumb(id = itemId, name = name.ifEmpty { itemId }))
        load(itemId) { filesRepository.getChildren(itemId, "Bearer $token").filter { it.folder != null } }
    }

    fun goBack(token: String) {
        if (stack.size <= 1) return
        stack.removeAt(stack.lastIndex)
        val key = stack.last().id
        cache[key]?.let { setState(it) } ?: run {
            load(key) {
                if (key == rootId || key == "root") filesRepository.getRootChildren("Bearer $token") else filesRepository.getChildren(key, "Bearer $token")
            }.let { /* launched */ }
        }
    }

    fun currentFolderId(): String = stack.lastOrNull()?.id ?: (rootId ?: "root")

    private fun load(key: String, block: suspend () -> List<DriveItemDto>) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, canGoBack = stack.size > 1, path = stack.toList())
            try {
                val items = block().filter { it.folder != null }
                cache[key] = items
                setState(items)
            } catch (e: Exception) {
                _state.value = FolderPickerUiState(null, false, e.message, stack.size > 1, stack.toList())
            }
        }
    }

    private fun setState(items: List<DriveItemDto>) {
        _state.value = FolderPickerUiState(
            items = items,
            isLoading = false,
            error = null,
            canGoBack = stack.size > 1,
            path = stack.toList()
        )
    }
}

