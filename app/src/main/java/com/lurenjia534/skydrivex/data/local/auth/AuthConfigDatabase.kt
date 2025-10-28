package com.lurenjia534.skydrivex.data.local.auth

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AuthConfigEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AuthConfigDatabase : RoomDatabase() {
    abstract fun authConfigDao(): AuthConfigDao
}
