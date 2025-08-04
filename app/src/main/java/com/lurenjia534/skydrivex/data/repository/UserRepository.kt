package com.lurenjia534.skydrivex.data.repository

import com.lurenjia534.skydrivex.data.model.UserDto
import com.lurenjia534.skydrivex.data.remote.GraphApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val api: GraphApiService
) {
    suspend fun getUser(token: String): UserDto = api.getMe(token)
}

