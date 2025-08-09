package com.lurenjia534.skydrivex.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DriveItemDownloadUrlDto(
    @param:Json(name = "@microsoft.graph.downloadUrl") val downloadUrl: String?
)

