package com.lurenjia534.skydrivex.data.local.photosync

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "album_remote_folders")
data class AlbumFolderEntity(
    @PrimaryKey val bucketId: String,
    val albumName: String,
    val remoteFolderId: String,
    val remotePath: String,
    val updatedAt: Long
)
