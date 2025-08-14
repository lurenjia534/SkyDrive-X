package com.lurenjia534.skydrivex.data.remote

import com.lurenjia534.skydrivex.data.model.drive.DriveDto
import com.lurenjia534.skydrivex.data.model.driveitem.DriveItemsResponse
import com.lurenjia534.skydrivex.data.model.user.UserDto
import com.lurenjia534.skydrivex.data.model.driveitem.DriveItemDownloadUrlDto
import com.lurenjia534.skydrivex.data.model.permission.CreateLinkRequest
import com.lurenjia534.skydrivex.data.model.permission.CreateLinkResponse
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GraphApiService {
    @DELETE("me/drive/items/{itemId}")
    suspend fun deleteFile(
        @Path("itemId") id: String,
        @Header("Authorization") token: String
    ): Response<Unit>
    @GET("me")
    suspend fun getMe(@Header("Authorization") token: String): UserDto

    @GET("me/drive?\$select=id,driveType,quota")
    suspend fun getDrive(@Header("Authorization") token: String): DriveDto

    @GET("me/drive/root/children")
    suspend fun getRootChildren(
        @Header("Authorization") token: String,
        @Query("\$top") top: Int? = null,
        @Query("\$orderby") orderBy: String? = null
    ): DriveItemsResponse

    @GET("me/drive/items/{itemId}/children")
    suspend fun getChildren(
        @Path("itemId") id: String,
        @Header("Authorization") token: String,
        @Query("\$top") top: Int? = null,
        @Query("\$orderby") orderBy: String? = null
    ): DriveItemsResponse

    // Fetch pre-authenticated download URL for a file
    @GET("me/drive/items/{itemId}?\$select=@microsoft.graph.downloadUrl")
    suspend fun getDownloadUrl(
        @Path("itemId") id: String,
        @Header("Authorization") token: String
    ): DriveItemDownloadUrlDto

    // Create a sharing link for an item
    @POST("me/drive/items/{itemId}/createLink")
    suspend fun createLink(
        @Path("itemId") id: String,
        @Header("Authorization") token: String,
        @Body body: CreateLinkRequest
    ): CreateLinkResponse
}
