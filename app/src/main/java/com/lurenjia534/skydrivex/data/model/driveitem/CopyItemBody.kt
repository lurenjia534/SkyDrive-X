package com.lurenjia534.skydrivex.data.model.driveitem

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CopyItemBody(
    val parentReference: CopyParentReference? = null,
    val name: String? = null
)

@JsonClass(generateAdapter = true)
data class CopyParentReference(
    val driveId: String?,
    val id: String?
)

@JsonClass(generateAdapter = true)
data class CopyMonitorStatus(
    val status: String? = null,        // e.g., inProgress | completed | failed
    val percentageComplete: Double? = null,
    val resourceId: String? = null,    // id of the created copy
    val resourceLocation: String? = null,
    val error: Any? = null
)

