package com.lurenjia534.skydrivex.data.remote

import com.lurenjia534.skydrivex.data.model.SharePointResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface SharePointApiService {
    @GET
    suspend fun getRecycleBinItems(
        @Url url: String,
        @Header("Authorization") token: String,
        @Header("Accept") accept: String = "application/json;odata=verbose"
    ): SharePointResponse

    @POST
    suspend fun restoreRecycleBinItem(
        @Url url: String,
        @Header("Authorization") token: String,
        @Header("Accept") accept: String = "application/json;odata=verbose"
    ): Response<Unit>

    @POST
    suspend fun deleteRecycleBinItem(
        @Url url: String,
        @Header("Authorization") token: String,
        @Header("Accept") accept: String = "application/json;odata=verbose"
    ): Response<Unit>
}
