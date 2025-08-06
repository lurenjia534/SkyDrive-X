package com.lurenjia534.skydrivex.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DriveItemDto(
    val id: String?,
    val name: String?,
    val size: Long?,
    val folder: FolderFacet?,
    val file: FileFacet?,
    val parentReference: ItemReference?
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
