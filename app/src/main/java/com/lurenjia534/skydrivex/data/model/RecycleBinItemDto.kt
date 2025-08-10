package com.lurenjia534.skydrivex.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RecycleBinItemDto(
    @Json(name = "Id") val id: String,
    @Json(name = "Title") val title: String?,
    @Json(name = "Author") val author: UserIdentity?,
    @Json(name = "DeletedDate") val deletedDate: String?,
    @Json(name = "DirName") val originalLocation: String?,
    @Json(name = "ItemState") val itemState: String?,
    @Json(name = "ItemType") val itemType: String?,
    @Json(name = "Size") val size: Long?
)

@JsonClass(generateAdapter = true)
data class UserIdentity(
    @Json(name = "Title") val displayName: String?
)
