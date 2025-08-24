package com.lurenjia534.skydrivex.data.model.driveitem

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateFolderBody(
    val name: String,
    val folder: EmptyFolder = EmptyFolder(),
    @Json(name = "@microsoft.graph.conflictBehavior") val conflictBehavior: String = "rename"
)

@JsonClass(generateAdapter = true)
data class EmptyFolder(val any: String? = null)

