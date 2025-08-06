package com.lurenjia534.skydrivex.data.repository

import com.lurenjia534.skydrivex.data.model.DriveItemDto
import com.lurenjia534.skydrivex.data.remote.GraphApiService
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
}
