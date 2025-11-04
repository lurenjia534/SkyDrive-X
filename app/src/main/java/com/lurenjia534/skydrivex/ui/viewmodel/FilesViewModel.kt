package com.lurenjia534.skydrivex.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurenjia534.skydrivex.data.model.driveitem.DriveItemDto
import com.lurenjia534.skydrivex.data.repository.FilesRepository
import com.lurenjia534.skydrivex.data.repository.BatchMoveResult
import dagger.hilt.android.lifecycle.HiltViewModel
import com.lurenjia534.skydrivex.ui.state.FilesUiState
import com.lurenjia534.skydrivex.ui.state.Breadcrumb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FilesViewModel @Inject constructor(
    private val filesRepository: FilesRepository,
) : ViewModel() {

    private var currentToken: String? = null
    private var rootDriveId: String? = null

    private val _filesState = MutableStateFlow(
        FilesUiState(items = null, isLoading = false, error = null, canGoBack = false, path = emptyList())
    )
    val filesState: StateFlow<FilesUiState> = _filesState.asStateFlow()

    private val cache = mutableMapOf<String, List<DriveItemDto>>()
    private val stack = mutableListOf<Breadcrumb>()

    private fun visibleItemIds(): Set<String> {
        val state = _filesState.value
        val source = state.searchResults ?: state.items
        return source.orEmpty().mapNotNull { it.id }.toSet()
    }

    private suspend fun ensureRootId(token: String) {
        if (rootDriveId != null) return
        val resolved = runCatching {
            filesRepository.getRootId("Bearer $token")
        }.getOrNull()
        if (!resolved.isNullOrBlank()) {
            rootDriveId = resolved
        }
    }

    private suspend fun cacheKeyFor(token: String, folderId: String): String {
        ensureRootId(token)
        val normalized = folderId.ifBlank { "root" }
        val rootId = rootDriveId
        return when {
            normalized == "root" -> "root"
            rootId != null && normalized == rootId -> "root"
            else -> normalized
        }
    }

    private fun clearDestinationCache(cacheKey: String, folderId: String) {
        cache.remove(cacheKey)
        if (cacheKey != folderId) {
            cache.remove(folderId)
        }
    }

    fun enterSelectionMode(initial: String? = null) {
        _filesState.value = _filesState.value.copy(
            selectionMode = true,
            selectedIds = buildSet {
                if (!initial.isNullOrEmpty()) add(initial)
            }
        )
    }

    fun exitSelectionMode() {
        val state = _filesState.value
        if (!state.selectionMode && state.selectedIds.isEmpty()) return
        _filesState.value = state.copy(selectionMode = false, selectedIds = emptySet())
    }

    fun toggleSelect(id: String) {
        _filesState.value = _filesState.value.let { state ->
            val newSet = if (state.selectedIds.contains(id)) state.selectedIds - id else state.selectedIds + id
            state.copy(selectedIds = newSet, selectionMode = true)
        }
    }

    fun selectAllCurrent() {
        val all = visibleItemIds()
        _filesState.value = _filesState.value.copy(selectionMode = all.isNotEmpty(), selectedIds = all)
    }

    fun invertSelection() {
        val all = visibleItemIds()
        val current = _filesState.value.selectedIds
        val inverted = all - current
        _filesState.value = _filesState.value.copy(selectionMode = all.isNotEmpty(), selectedIds = inverted)
    }

    fun loadRoot(token: String, force: Boolean = false) {
        if (!force && currentToken == token && stack.isNotEmpty()) {
            return
        }
        val tokenChanged = currentToken != token
        currentToken = token
        exitSelectionMode()
        stack.clear()
        stack.add(Breadcrumb(id = "root", name = "根目录"))
        if (tokenChanged) {
            cache.clear()
            rootDriveId = null
        }
        load("root") { filesRepository.getRootChildren("Bearer $token") }
    }

    fun loadChildren(itemId: String, token: String, name: String) {
        exitSelectionMode()
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
        exitSelectionMode()
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

    // --- Search API (server-side) ---
    fun clearSearch() {
        exitSelectionMode()
        _filesState.value = _filesState.value.copy(
            searchResults = null,
            isSearching = false,
            searchError = null
        )
    }

    fun searchInDrive(token: String, query: String, top: Int? = 50) {
        if (query.isBlank()) {
            clearSearch()
            return
        }
        viewModelScope.launch {
            _filesState.value = _filesState.value.copy(isSearching = true, searchError = null)
            try {
                val (items, _next) = filesRepository.searchInDriveWithThumbnails(
                    token = "Bearer $token",
                    query = query,
                    top = top
                )
                _filesState.value = _filesState.value.copy(
                    searchResults = items,
                    isSearching = false,
                    searchError = null
                )
            } catch (e: Exception) {
                _filesState.value = _filesState.value.copy(
                    isSearching = false,
                    searchError = e.message
                )
            }
        }
    }

    fun searchInCurrentFolder(token: String, query: String, top: Int? = 50) {
        val folderId = currentFolderId()
        if (query.isBlank()) {
            clearSearch()
            return
        }
        viewModelScope.launch {
            _filesState.value = _filesState.value.copy(isSearching = true, searchError = null)
            try {
                val (items, _next) = if (folderId == "root") {
                    filesRepository.searchInDriveWithThumbnails(
                        token = "Bearer $token",
                        query = query,
                        top = top
                    )
                } else {
                    filesRepository.searchInFolderWithThumbnails(
                        folderId = folderId,
                        token = "Bearer $token",
                        query = query,
                        top = top
                    )
                }
                _filesState.value = _filesState.value.copy(
                    searchResults = items,
                    isSearching = false,
                    searchError = null
                )
            } catch (e: Exception) {
                _filesState.value = _filesState.value.copy(
                    isSearching = false,
                    searchError = e.message
                )
            }
        }
    }

    fun navigateTo(index: Int, token: String) {
        exitSelectionMode()
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

    fun deleteSelected(token: String) {
        val ids = _filesState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                if (ids.size <= 4) {
                    coroutineScope {
                        ids.map { id ->
                            async(Dispatchers.IO) {
                                filesRepository.deleteFile(id, "Bearer $token")
                            }
                        }.awaitAll()
                    }
                } else {
                    filesRepository.deleteFilesBatch(ids, "Bearer $token")
                }
                exitSelectionMode()
                refreshCurrent(token)
            } catch (e: Exception) {
                _filesState.value = _filesState.value.copy(error = e.message)
            }
        }
    }

    suspend fun moveSelected(
        token: String,
        newParentId: String,
        renameMap: Map<String, String?> = emptyMap()
    ): BatchMoveResult {
        val ids = _filesState.value.selectedIds.toList()
        if (ids.isEmpty()) return BatchMoveResult(0, emptyList(), emptyList())
        val result = filesRepository.moveItems(
            itemIds = ids,
            token = "Bearer $token",
            newParentId = newParentId,
            renameMap = renameMap
        )
        val destCacheKey = cacheKeyFor(token, newParentId)
        if (result.succeeded.isNotEmpty()) {
            clearDestinationCache(destCacheKey, newParentId)
        }
        exitSelectionMode()
        refreshCurrent(token)
        if (result.succeeded.isNotEmpty() && destCacheKey != currentFolderId()) {
            val refreshId = if (destCacheKey == "root") "" else newParentId
            refreshFolder(token, refreshId)
        }
        return result
    }

    suspend fun getDownloadUrl(itemId: String, token: String): String? {
        return filesRepository.getDownloadUrl(itemId, "Bearer $token")
    }

    suspend fun getItemDetails(itemId: String, token: String): com.lurenjia534.skydrivex.data.model.driveitem.DriveItemDto {
        return filesRepository.getItemDetails(itemId, "Bearer $token")
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

    fun refreshCurrent(token: String) {
        refreshFolder(token = token, folderId = currentFolderId())
    }

    fun refreshFolder(token: String, folderId: String) {
        val targetId = folderId.ifEmpty { "root" }
        cache.remove(targetId)
        val isCurrentFolder = currentFolderId() == targetId
        viewModelScope.launch {
            if (isCurrentFolder) {
                _filesState.value = _filesState.value.copy(isLoading = true, error = null)
            }
            try {
                val items = if (targetId == "root") {
                    filesRepository.getRootChildren("Bearer $token")
                } else {
                    filesRepository.getChildren(targetId, "Bearer $token")
                }
                cache[targetId] = items
                if (isCurrentFolder) {
                    _filesState.value = _filesState.value.copy(
                        items = items,
                        isLoading = false,
                        error = null,
                        canGoBack = stack.size > 1,
                        path = stack.toList()
                    )
                }
            } catch (e: Exception) {
                if (isCurrentFolder) {
                    _filesState.value = _filesState.value.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

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

    suspend fun moveItem(
        itemId: String,
        token: String,
        newParentId: String,
        newName: String? = null
    ): DriveItemDto {
        val moved = filesRepository.moveItem(
            itemId = itemId,
            token = "Bearer $token",
            newParentId = newParentId,
            newName = newName
        )
        val destCacheKey = cacheKeyFor(token, newParentId)
        clearDestinationCache(destCacheKey, newParentId)
        // 移动后刷新当前目录（无论移动入/移出，至少确保当前视图更新）
        refreshCurrent(token)
        if (destCacheKey != currentFolderId()) {
            val refreshId = if (destCacheKey == "root") "" else newParentId
            refreshFolder(token, refreshId)
        }
        return moved
    }

    suspend fun copyItem(
        itemId: String,
        token: String,
        newParentId: String,
        newName: String? = null
    ): DriveItemDto? {
        val copied = filesRepository.copyItem(
            itemId = itemId,
            token = "Bearer $token",
            newParentId = newParentId,
            newName = newName
        )
        // Refresh current directory to reflect new copy if copied into it
        refreshCurrent(token)
        return copied
    }

    suspend fun renameItem(
        itemId: String,
        token: String,
        newName: String
    ): DriveItemDto {
        val renamed = filesRepository.renameItem(
            itemId = itemId,
            token = "Bearer $token",
            newName = newName
        )
        refreshCurrent(token)
        return renamed
    }
}
