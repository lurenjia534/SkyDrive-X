package com.lurenjia534.skydrivex.data.model.driveitem

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MoveItemBody(
    val parentReference: ParentReferenceUpdate? = null,
    val name: String? = null
)

@JsonClass(generateAdapter = true)
data class ParentReferenceUpdate(
    val id: String?
)

