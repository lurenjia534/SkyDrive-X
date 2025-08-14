package com.lurenjia534.skydrivex.data.model.permission

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateLinkResponse(
    val link: PermissionLink? = null,
)

@JsonClass(generateAdapter = true)
data class PermissionLink(
    val type: String? = null,  // view | edit | embed
    val webUrl: String? = null
)

