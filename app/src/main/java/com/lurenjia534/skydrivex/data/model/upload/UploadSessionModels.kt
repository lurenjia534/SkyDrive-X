package com.lurenjia534.skydrivex.data.model.upload

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UploadSessionDto(
    val uploadUrl: String,
    val expirationDateTime: String,
    val nextExpectedRanges: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class CreateUploadSessionRequest(
    val item: DriveItemUploadableProperties? = DriveItemUploadableProperties(),
    val deferCommit: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class DriveItemUploadableProperties(
    @Json(name = "@microsoft.graph.conflictBehavior") val conflictBehavior: String? = "rename",
    val description: String? = null,
    val fileSize: Long? = null,
    val name: String? = null
)

