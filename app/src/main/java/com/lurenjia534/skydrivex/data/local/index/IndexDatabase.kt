package com.lurenjia534.skydrivex.data.local.index

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [IndexItemEntity::class, IndexItemFts::class],
    version = 1,
    exportSchema = false
)
abstract class IndexDatabase : RoomDatabase() {
    abstract fun indexDao(): IndexDao
}

