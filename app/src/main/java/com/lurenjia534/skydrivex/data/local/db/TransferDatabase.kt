package com.lurenjia534.skydrivex.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lurenjia534.skydrivex.data.local.db.converter.TransferStatusRoomConverter
import com.lurenjia534.skydrivex.data.local.db.converter.TransferTypeRoomConverter
import com.lurenjia534.skydrivex.data.local.db.dao.TransferDao

@Database(
    entities = [TransferEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(TransferTypeRoomConverter::class, TransferStatusRoomConverter::class)
abstract class TransferDatabase : RoomDatabase() {
    abstract fun transferDao(): TransferDao
}
