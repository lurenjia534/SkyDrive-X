package com.lurenjia534.skydrivex.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SharePointResponse(
    val d: Results<RecycleBinItemDto>
)

@JsonClass(generateAdapter = true)
data class Results<T>(
    val results: List<T>
)
