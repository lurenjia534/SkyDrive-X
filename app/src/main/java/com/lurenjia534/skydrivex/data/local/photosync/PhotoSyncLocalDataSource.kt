package com.lurenjia534.skydrivex.data.local.photosync

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class PhotoSyncLocalDataSource @Inject constructor(
    private val dao: PhotoSyncDao
) {

    fun observeAlbumPreferences(): Flow<List<AlbumPreferenceEntity>> = dao.observeAlbumPreferences()

    suspend fun getAlbumPreference(bucketId: String): AlbumPreferenceEntity? =
        dao.getAlbumPreference(bucketId)

    suspend fun setAlbumPreference(bucketId: String, albumName: String, enabled: Boolean) {
        dao.upsertAlbumPreference(
            AlbumPreferenceEntity(
                bucketId = bucketId,
                albumName = albumName,
                enabled = enabled,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getAlbumFolder(bucketId: String): AlbumFolderEntity? =
        dao.getAlbumFolder(bucketId)

    suspend fun setAlbumFolder(bucketId: String, albumName: String, folderId: String, remotePath: String) {
        dao.upsertAlbumFolder(
            AlbumFolderEntity(
                bucketId = bucketId,
                albumName = albumName,
                remoteFolderId = folderId,
                remotePath = remotePath,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getMediaUpload(contentUri: String): MediaUploadEntity? =
        dao.getMediaUpload(contentUri)

    suspend fun markUpload(
        contentUri: String,
        bucketId: String,
        displayName: String?,
        size: Long,
        dateModified: Long,
        remoteItemId: String?,
        status: UploadStatus
    ) {
        dao.upsertMediaUpload(
            MediaUploadEntity(
                contentUri = contentUri,
                bucketId = bucketId,
                displayName = displayName,
                size = size,
                dateModified = dateModified,
                remoteItemId = remoteItemId,
                status = status,
                lastAttemptAt = System.currentTimeMillis()
            )
        )
    }
}
