package com.lurenjia534.skydrivex.data.repository

import com.lurenjia534.skydrivex.data.local.auth.AuthConfigDao
import com.lurenjia534.skydrivex.data.local.auth.AuthConfigEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

data class AuthConfig(
    val clientId: String
)

@Singleton
class AuthConfigRepository @Inject constructor(
    private val authConfigDao: AuthConfigDao
) {

    val configFlow: Flow<AuthConfig?> = authConfigDao
        .observeConfig()
        .map { entity -> entity?.toDomain() }
        .distinctUntilChanged()

    suspend fun getConfig(): AuthConfig? = authConfigDao.getConfig()?.toDomain()

    suspend fun hasConfig(): Boolean = authConfigDao.getConfig() != null

    suspend fun saveClientId(clientId: String) {
        val entity = AuthConfigEntity(clientId = clientId)
        authConfigDao.upsert(entity)
    }

    suspend fun clear() {
        authConfigDao.clear()
    }

    private fun AuthConfigEntity.toDomain(): AuthConfig = AuthConfig(
        clientId = clientId
    )
}
