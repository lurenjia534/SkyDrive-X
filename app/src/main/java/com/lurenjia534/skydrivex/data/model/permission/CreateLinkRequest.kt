package com.lurenjia534.skydrivex.data.model.permission

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateLinkRequest(
    val type: String,                 // view | edit | embed
    val scope: String? = null,        // anonymous | organization
    val password: String? = null,     // Only supported for personal + anonymous
    val expirationDateTime: String? = null // RFC3339 e.g. 2025-12-31T23:59:59Z
)

