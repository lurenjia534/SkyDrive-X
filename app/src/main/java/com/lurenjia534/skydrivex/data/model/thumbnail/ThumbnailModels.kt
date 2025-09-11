package com.lurenjia534.skydrivex.data.model.thumbnail

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Thumbnail(
    val height: Int?,
    val width: Int?,
    val url: String?
)

@JsonClass(generateAdapter = true)
data class ThumbnailSet(
    val id: String? = null,
    val small: Thumbnail? = null,
    val medium: Thumbnail? = null,
    val large: Thumbnail? = null,
    val smallSquare: Thumbnail? = null,
    val mediumSquare: Thumbnail? = null,
    val largeSquare: Thumbnail? = null,
    // 常用自定义示例，可为空；解析不到时忽略
    @param:Json(name = "c200x200_crop") val c200x200Crop: Thumbnail? = null
)

@JsonClass(generateAdapter = true)
data class ThumbnailSetsResponse(
    val value: List<ThumbnailSet>
)

