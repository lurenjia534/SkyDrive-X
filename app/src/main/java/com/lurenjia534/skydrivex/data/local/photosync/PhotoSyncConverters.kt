package com.lurenjia534.skydrivex.data.local.photosync

import androidx.room.TypeConverter

class PhotoSyncConverters {
    @TypeConverter
    fun fromStatus(status: UploadStatus?): String? = status?.name

    @TypeConverter
    fun toStatus(value: String?): UploadStatus? = value?.let { UploadStatus.valueOf(it) }
}
