package com.lurenjia534.skydrivex.data.local.db.model

import androidx.room.TypeConverter

enum class TransferType {
    DOWNLOAD_SYSTEM,
    DOWNLOAD_CUSTOM,
    UPLOAD
}

class TransferTypeConverter {
    @TypeConverter
    fun toStored(value: TransferType): String = value.name

    @TypeConverter
    fun fromStored(value: String): TransferType = enumValueOf(value)
}
