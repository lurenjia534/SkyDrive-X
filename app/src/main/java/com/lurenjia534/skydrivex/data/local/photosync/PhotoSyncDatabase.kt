package com.lurenjia534.skydrivex.data.local.photosync

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        AlbumPreferenceEntity::class,
        AlbumFolderEntity::class,
        MediaUploadEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(PhotoSyncConverters::class)
abstract class PhotoSyncDatabase : RoomDatabase() {
    abstract fun photoSyncDao(): PhotoSyncDao
}
