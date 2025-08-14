package com.lurenjia534.skydrivex.data.repository

import com.lurenjia534.skydrivex.data.model.driveitem.DriveItemDto
import com.lurenjia534.skydrivex.data.remote.GraphApiService
import com.lurenjia534.skydrivex.data.model.permission.CreateLinkRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilesRepository @Inject constructor(
    private val graphApiService: GraphApiService
) {
    suspend fun getRootChildren(token: String): List<DriveItemDto> =
        graphApiService.getRootChildren(token).value

    suspend fun getChildren(itemId: String, token: String): List<DriveItemDto> =
        graphApiService.getChildren(itemId, token).value

    suspend fun deleteFile(itemId: String, token: String) {
        graphApiService.deleteFile(id = itemId, token = token)
    }

    suspend fun getDownloadUrl(itemId: String, token: String): String? =
        graphApiService.getDownloadUrl(id = itemId, token = token).downloadUrl

    suspend fun createShareLink(
        itemId: String,
        token: String,
        type: String,                 // view | edit | embed
        scope: String?,               // anonymous | organization
        password: String?,            // only valid for personal + anonymous
        expirationDateTime: String?   // RFC3339 or null
    ): String? {
        val body = CreateLinkRequest(
            type = type,
            scope = scope,
            password = password,
            expirationDateTime = expirationDateTime
        )
        val resp = graphApiService.createLink(id = itemId, token = token, body = body)
        return resp.link?.webUrl
    }
}
