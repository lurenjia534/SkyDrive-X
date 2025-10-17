package com.lurenjia534.skydrivex.data.model.driveitem

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DriveDeltaResponse(
    val value: List<DriveItemDto>,
    @Json(name = "@odata.nextLink") val nextLink: String? = null,
    @Json(name = "@odata.deltaLink") val deltaLink: String? = null
)

