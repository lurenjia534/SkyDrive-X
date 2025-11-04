package com.lurenjia534.skydrivex.ui.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurenjia534.skydrivex.data.local.media.LocalAlbum
import com.lurenjia534.skydrivex.data.local.media.LocalMediaItem
import com.lurenjia534.skydrivex.data.local.photosync.PhotoSyncLocalDataSource
import com.lurenjia534.skydrivex.data.repository.PhotoSyncRepository
import com.lurenjia534.skydrivex.ui.state.PhotoSyncUiState
import com.lurenjia534.skydrivex.work.PhotoBackupScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PhotoSyncViewModel @Inject constructor(
    private val repository: PhotoSyncRepository,
    private val localDataSource: PhotoSyncLocalDataSource,
    private val scheduler: PhotoBackupScheduler,
    @ApplicationContext private val context: Context,
    private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhotoSyncUiState())
    val uiState = _uiState.asStateFlow()

    data class AlbumSection(
        val title: String,
        val items: List<LocalMediaItem>
    )

    data class AlbumDetailUiState(
        val album: LocalAlbum? = null,
        val isLoading: Boolean = false,
        val sections: List<AlbumSection> = emptyList(),
        val error: String? = null,
        val backupEnabled: Boolean = false
    )

    private val _albumDetailState = MutableStateFlow(AlbumDetailUiState())
    val albumDetailState = _albumDetailState.asStateFlow()

    init {
        viewModelScope.launch {
            localDataSource.observeAlbumPreferences().collect { prefs ->
                val mapping = prefs.associate { it.bucketId to it.enabled }
                _uiState.value = _uiState.value.copy(backupFlags = mapping)
                val current = _albumDetailState.value.album
                if (current != null) {
                    val enabled = mapping[current.bucketId] ?: false
                    _albumDetailState.value = _albumDetailState.value.copy(backupEnabled = enabled)
                }
            }
        }
        refresh()
    }

    fun refresh() {
        val granted = hasMediaPermission()
        _uiState.value = _uiState.value.copy(permissionGranted = granted, error = null)
        if (granted) {
            loadAlbums()
        }
    }

    fun requiredPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES
        )
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun onPermissionResult(result: Map<String, Boolean>) {
        val granted = result.values.any { it }
        _uiState.value = _uiState.value.copy(permissionGranted = granted)
        if (granted) {
            loadAlbums()
            _albumDetailState.value.album?.let { existing ->
                // reload current album content after permission grant
                openAlbum(existing)
            }
        }
    }

    private fun loadAlbums() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch(ioDispatcher) {
            runCatching { repository.loadAlbums() }
                .onSuccess { albums ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        albums = albums
                    )
                    val currentAlbum = _albumDetailState.value.album
                    if (currentAlbum != null) {
                        val refreshed = albums.firstOrNull { it.bucketId == currentAlbum.bucketId }
                        if (refreshed != null) {
                            openAlbum(refreshed)
                        } else {
                            _albumDetailState.value = AlbumDetailUiState()
                        }
                    }
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = throwable.message ?: "无法加载相册",
                        albums = emptyList()
                    )
                }
        }
    }

    private fun hasMediaPermission(): Boolean {
        val granted = { permission: String ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                granted(Manifest.permission.READ_MEDIA_IMAGES) ||
                    granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                granted(Manifest.permission.READ_MEDIA_IMAGES)
            else ->
                granted(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun openAlbum(album: LocalAlbum) {
        val backupEnabled = _uiState.value.backupFlags[album.bucketId] ?: false
        _albumDetailState.value = AlbumDetailUiState(
            album = album,
            isLoading = true,
            backupEnabled = backupEnabled
        )
        viewModelScope.launch(ioDispatcher) {
            runCatching { repository.loadAlbumItems(album.bucketId) }
                .onSuccess { items ->
                    val sections = buildSections(items)
                    _albumDetailState.value = AlbumDetailUiState(
                        album = album,
                        isLoading = false,
                        sections = sections,
                        backupEnabled = backupEnabled
                    )
                }
                .onFailure { throwable ->
                    _albumDetailState.value = AlbumDetailUiState(
                        album = album,
                        isLoading = false,
                        sections = emptyList(),
                        error = throwable.message ?: "无法读取相册内容",
                        backupEnabled = backupEnabled
                    )
                }
        }
    }

    fun closeAlbum() {
        _albumDetailState.value = AlbumDetailUiState()
    }

    fun toggleBackup(enabled: Boolean) {
        val current = _albumDetailState.value.album ?: return
        _albumDetailState.value = _albumDetailState.value.copy(backupEnabled = enabled)
        viewModelScope.launch(ioDispatcher) {
            localDataSource.setAlbumPreference(
                bucketId = current.bucketId,
                albumName = current.displayName ?: current.bucketId,
                enabled = enabled
            )
        }
        if (enabled) {
            scheduler.enqueueAlbum(current.bucketId, current.displayName ?: current.bucketId)
        } else {
            scheduler.cancelAlbum(current.bucketId)
        }
    }

    fun backupNow() {
        val current = _albumDetailState.value.album ?: return
        scheduler.enqueueAlbum(current.bucketId, current.displayName ?: current.bucketId)
    }

    private fun buildSections(items: List<LocalMediaItem>): List<AlbumSection> {
        if (items.isEmpty()) return emptyList()
        val zone = ZoneId.systemDefault()
        val formatter = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.getDefault())
        val grouped = items.groupBy { media ->
            val instant = Instant.ofEpochMilli(media.takenAtMillis.takeIf { it > 0 } ?: System.currentTimeMillis())
            instant.atZone(zone).toLocalDate()
        }
        return grouped.toSortedMap(compareByDescending<LocalDate> { it }).map { (date, mediaList) ->
            AlbumSection(
                title = formatter.format(date),
                items = mediaList
            )
        }
    }
}
