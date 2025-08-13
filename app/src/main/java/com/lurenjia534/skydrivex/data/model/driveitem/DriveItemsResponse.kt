package com.lurenjia534.skydrivex.data.model.driveitem

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DriveItemsResponse(
    val value: List<DriveItemDto>
)
