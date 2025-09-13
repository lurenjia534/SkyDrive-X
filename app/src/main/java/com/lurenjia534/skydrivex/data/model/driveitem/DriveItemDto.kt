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
    val remoteItem: RemoteItemFacet? = null,
    // 可选的缩略图集合（当使用 $expand=thumbnails 或后续补充时）
    val thumbnails: List<com.lurenjia534.skydrivex.data.model.thumbnail.ThumbnailSet>? = null,
    // 详情对话框需要的额外字段（可选，未选择时为 null）
    val webUrl: String? = null,
    val createdDateTime: String? = null,
    val lastModifiedDateTime: String? = null
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
