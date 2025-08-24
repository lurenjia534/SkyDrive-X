package com.lurenjia534.skydrivex.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurenjia534.skydrivex.data.model.driveitem.DriveItemDto
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
        FilesUiState(items = null, isLoading = false, error = null, canGoBack = false, path = emptyList())
    )
    val filesState: StateFlow<FilesUiState> = _filesState.asStateFlow()

    private val cache = mutableMapOf<String, List<DriveItemDto>>()
    private val stack = mutableListOf<Breadcrumb>()

    fun loadRoot(token: String) {
        stack.clear()
        stack.add(Breadcrumb(id = "root", name = "根目录"))
        load("root") { filesRepository.getRootChildren("Bearer $token") }
    }

    fun loadChildren(itemId: String, token: String, name: String) {
        stack.add(Breadcrumb(id = itemId, name = name.ifEmpty { itemId }))
        load(itemId) { filesRepository.getChildren(itemId, "Bearer $token") }
    }

    private fun load(key: String, request: suspend () -> List<DriveItemDto>) {
        cache[key]?.let {
            _filesState.value = FilesUiState(
                items = it,
                isLoading = false,
                error = null,
                canGoBack = stack.size > 1,
                path = stack.toList()
            )
            return
        }
        viewModelScope.launch {
            _filesState.value = FilesUiState(
                items = null,
                isLoading = true,
                error = null,
                canGoBack = stack.size > 1,
                path = stack.toList()
            )
            try {
                val items = request()
                cache[key] = items
                _filesState.value = FilesUiState(
                    items = items,
                    isLoading = false,
                    error = null,
                    canGoBack = stack.size > 1,
                    path = stack.toList()
                )
            } catch (e: Exception) {
                // 回退栈（如果此次加载是深入子层级失败）
                if (stack.isNotEmpty() && stack.last().id == key && stack.size > 1) {
                    stack.removeAt(stack.lastIndex)
                }
                _filesState.value = FilesUiState(
                    items = null,
                    isLoading = false,
                    error = e.message,
                    canGoBack = stack.size > 1,
                    path = stack.toList()
                )
            }
        }
    }

    fun goBack(token: String) {
        if (stack.size <= 1) return
        // 弹出当前层级（避免使用 API 35 的 List#removeLast）
        stack.removeAt(stack.lastIndex)
        val key = stack.last().id
        cache[key]?.let { cached ->
            _filesState.value = FilesUiState(
                items = cached,
                isLoading = false,
                error = null,
                canGoBack = stack.size > 1,
                path = stack.toList()
            )
            return
        }
        viewModelScope.launch {
            _filesState.value = FilesUiState(
                items = null,
                isLoading = true,
                error = null,
                canGoBack = stack.size > 1,
                path = stack.toList()
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
                    canGoBack = stack.size > 1,
                    path = stack.toList()
                )
            } catch (e: Exception) {
                _filesState.value = FilesUiState(
                    items = null,
                    isLoading = false,
                    error = e.message,
                    canGoBack = stack.size > 1,
                    path = stack.toList()
                )
            }
        }
    }

    fun navigateTo(index: Int, token: String) {
        if (index < 0 || index >= stack.size) return
        if (index == stack.lastIndex) return
        val target = stack[index]
        // 截断到 index
        while (stack.lastIndex > index) {
            stack.removeAt(stack.lastIndex)
        }
        val key = target.id
        cache[key]?.let { cached ->
            _filesState.value = FilesUiState(
                items = cached,
                isLoading = false,
                error = null,
                canGoBack = stack.size > 1,
                path = stack.toList()
            )
            return
        }
        viewModelScope.launch {
            _filesState.value = FilesUiState(
                items = null,
                isLoading = true,
                error = null,
                canGoBack = stack.size > 1,
                path = stack.toList()
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
                    canGoBack = stack.size > 1,
                    path = stack.toList()
                )
            } catch (e: Exception) {
                _filesState.value = FilesUiState(
                    items = null,
                    isLoading = false,
                    error = e.message,
                    canGoBack = stack.size > 1,
                    path = stack.toList()
                )
            }
        }
    }
    fun deleteFile(itemId: String, token: String) {
        viewModelScope.launch {
            try {
                filesRepository.deleteFile(itemId, "Bearer $token")
                // 从缓存中移除当前目录的缓存，以便下次重新加载
                val currentKey = stack.last().id
                cache.remove(currentKey)
                // 重新加载当前目录
                val items = if (currentKey == "root") {
                    filesRepository.getRootChildren("Bearer $token")
                } else {
                    filesRepository.getChildren(currentKey, "Bearer $token")
                }
                cache[currentKey] = items
                _filesState.value = _filesState.value.copy(items = items)
            } catch (e: Exception) {
                _filesState.value = _filesState.value.copy(error = e.message)
            }
        }
    }

    suspend fun getDownloadUrl(itemId: String, token: String): String? {
        return filesRepository.getDownloadUrl(itemId, "Bearer $token")
    }

    suspend fun createShareLink(
        itemId: String,
        token: String,
        type: String,
        scope: String?,
        password: String?,
        expirationDateTime: String?
    ): String? {
        return filesRepository.createShareLink(
            itemId = itemId,
            token = "Bearer $token",
            type = type,
            scope = scope,
            password = password,
            expirationDateTime = expirationDateTime
        )
    }

    fun currentFolderId(): String = stack.lastOrNull()?.id ?: "root"

    suspend fun uploadSmallFileToCurrent(
        token: String,
        fileName: String,
        mimeType: String?,
        bytes: ByteArray
    ): DriveItemDto {
        val parentId = currentFolderId()
        val item = filesRepository.uploadSmallFile(
            parentId = parentId,
            token = "Bearer $token",
            fileName = fileName,
            mimeType = mimeType,
            bytes = bytes
        )
        // Invalidate current folder cache and refresh
        cache.remove(parentId)
        // reload current level without altering stack
        val items = if (parentId == "root") {
            filesRepository.getRootChildren("Bearer $token")
        } else {
            filesRepository.getChildren(parentId, "Bearer $token")
        }
        cache[parentId] = items
        _filesState.value = FilesUiState(
            items = items,
            isLoading = false,
            error = null,
            canGoBack = stack.size > 1,
            path = stack.toList()
        )
        return item
    }

    suspend fun createFolderInCurrent(
        token: String,
        name: String,
        conflictBehavior: String = "rename"
    ): DriveItemDto {
        val parentId = currentFolderId()
        val item = filesRepository.createFolder(
            parentId = parentId,
            token = "Bearer $token",
            name = name,
            conflictBehavior = conflictBehavior
        )
        // refresh listing
        cache.remove(parentId)
        val items = if (parentId == "root") {
            filesRepository.getRootChildren("Bearer $token")
        } else {
            filesRepository.getChildren(parentId, "Bearer $token")
        }
        cache[parentId] = items
        _filesState.value = FilesUiState(
            items = items,
            isLoading = false,
            error = null,
            canGoBack = stack.size > 1,
            path = stack.toList()
        )
        return item
    }

    suspend fun uploadLargeFileToCurrent(
        token: String,
        fileName: String,
        totalBytes: Long,
        chunkProvider: suspend (offset: Long, size: Int) -> ByteArray,
        cancelFlag: java.util.concurrent.atomic.AtomicBoolean? = null,
        onProgress: ((uploaded: Long, total: Long) -> Unit)? = null
    ): DriveItemDto {
        val parentId = currentFolderId()
        val item = filesRepository.uploadLargeFile(
            parentId = parentId,
            token = "Bearer $token",
            fileName = fileName,
            totalBytes = totalBytes,
            bytesProvider = chunkProvider,
            cancelFlag = cancelFlag,
            onProgress = onProgress
        )
        // refresh
        cache.remove(parentId)
        val items = if (parentId == "root") {
            filesRepository.getRootChildren("Bearer $token")
        } else {
            filesRepository.getChildren(parentId, "Bearer $token")
        }
        cache[parentId] = items
        _filesState.value = FilesUiState(
            items = items,
            isLoading = false,
            error = null,
            canGoBack = stack.size > 1,
            path = stack.toList()
        )
        return item
    }
}
