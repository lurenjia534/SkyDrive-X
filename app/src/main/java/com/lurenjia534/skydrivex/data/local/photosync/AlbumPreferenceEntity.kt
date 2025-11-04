package com.lurenjia534.skydrivex.data.local.photosync

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "album_prefs")
data class AlbumPreferenceEntity(
    @PrimaryKey val bucketId: String,
    val albumName: String,
    val enabled: Boolean,
    val updatedAt: Long
)
