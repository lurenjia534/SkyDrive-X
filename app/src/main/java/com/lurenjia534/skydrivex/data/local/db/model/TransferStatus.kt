package com.lurenjia534.skydrivex.data.local.db.model

import androidx.room.TypeConverter

enum class TransferStatus {
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELLED
}

class TransferStatusConverter {
    @TypeConverter
    fun toStored(value: TransferStatus): String = value.name

    @TypeConverter
    fun fromStored(value: String): TransferStatus = enumValueOf(value)
}
