package com.lurenjia534.skydrivex.data.local.db.converter

import androidx.room.TypeConverter
import com.lurenjia534.skydrivex.data.local.db.model.TransferStatus
import com.lurenjia534.skydrivex.data.local.db.model.TransferType

class TransferTypeRoomConverter {
    @TypeConverter
    fun toStored(value: TransferType): String = value.name

    @TypeConverter
    fun fromStored(value: String): TransferType = enumValueOf(value)
}

class TransferStatusRoomConverter {
    @TypeConverter
    fun toStored(value: TransferStatus): String = value.name

    @TypeConverter
    fun fromStored(value: String): TransferStatus = enumValueOf(value)
}
