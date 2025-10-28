package com.lurenjia534.skydrivex.data.local.auth

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AuthConfigDao {

    @Query("SELECT * FROM auth_config WHERE id = :id LIMIT 1")
    fun observeConfig(id: Int = AuthConfigEntity.SINGLETON_ID): Flow<AuthConfigEntity?>

    @Query("SELECT * FROM auth_config WHERE id = :id LIMIT 1")
    suspend fun getConfig(id: Int = AuthConfigEntity.SINGLETON_ID): AuthConfigEntity?

    @Upsert
    suspend fun upsert(config: AuthConfigEntity)

    @Query("DELETE FROM auth_config")
    suspend fun clear()
}
