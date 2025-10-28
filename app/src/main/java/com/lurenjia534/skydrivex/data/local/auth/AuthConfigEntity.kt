package com.lurenjia534.skydrivex.data.local.auth

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "auth_config")
data class AuthConfigEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val clientId: String,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
