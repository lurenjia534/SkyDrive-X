package com.lurenjia534.skydrivex.data.model.driveitem

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DriveItemDto(
    val id: String?,
    val name: String?,
    val size: Long?,
    val folder: FolderFacet?,
    val file: FileFacet?,
    @param:Json(name = "parentReference") val parentReference: ItemReference?,
    val searchResult: SearchResultFacet? = null,
    val remoteItem: RemoteItemFacet? = null
)

@JsonClass(generateAdapter = true)
data class FolderFacet(
    val childCount: Int?
)

@JsonClass(generateAdapter = true)
data class FileFacet(
    val mimeType: String?
)

@JsonClass(generateAdapter = true)
data class ItemReference(
    val driveId: String?,
    val id: String?,
    val path: String?
)

@JsonClass(generateAdapter = true)
data class SearchResultFacet(
    val onClickTelemetryUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class RemoteItemFacet(
    val id: String?,
    @param:Json(name = "parentReference") val parentReference: ItemReference?
)
