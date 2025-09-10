package com.lurenjia534.skydrivex.data.model.driveitem

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DriveItemsResponse(
    val value: List<DriveItemDto>,
    @param:Json(name = "@odata.nextLink")   // 显式作用于构造参数
    val nextLink: String? = null
)
