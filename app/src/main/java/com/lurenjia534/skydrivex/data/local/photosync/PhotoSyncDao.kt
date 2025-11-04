package com.lurenjia534.skydrivex.data.local.photosync

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoSyncDao {

    @Query("SELECT * FROM album_prefs")
    fun observeAlbumPreferences(): Flow<List<AlbumPreferenceEntity>>

    @Query("SELECT * FROM album_prefs WHERE bucketId = :bucketId LIMIT 1")
    suspend fun getAlbumPreference(bucketId: String): AlbumPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlbumPreference(entity: AlbumPreferenceEntity)

    @Query("SELECT * FROM album_remote_folders WHERE bucketId = :bucketId LIMIT 1")
    suspend fun getAlbumFolder(bucketId: String): AlbumFolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlbumFolder(entity: AlbumFolderEntity)

    @Query("SELECT * FROM media_uploads WHERE contentUri = :contentUri LIMIT 1")
    suspend fun getMediaUpload(contentUri: String): MediaUploadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMediaUpload(entity: MediaUploadEntity)

    @Query("DELETE FROM media_uploads WHERE status = :status AND bucketId = :bucketId")
    suspend fun deleteByStatus(bucketId: String, status: UploadStatus)

    @Query("UPDATE media_uploads SET status = :status WHERE bucketId = :bucketId")
    suspend fun updateStatusForBucket(bucketId: String, status: UploadStatus)

    @Query("DELETE FROM media_uploads WHERE bucketId = :bucketId")
    suspend fun deleteByBucket(bucketId: String)
}
