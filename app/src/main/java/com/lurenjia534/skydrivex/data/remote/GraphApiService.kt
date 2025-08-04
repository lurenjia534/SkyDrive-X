package com.lurenjia534.skydrivex.data.remote

import com.lurenjia534.skydrivex.data.model.UserDto
import retrofit2.http.GET
import retrofit2.http.Header

interface GraphApiService {
    @GET("me")
    suspend fun getMe(@Header("Authorization") token: String): UserDto
}