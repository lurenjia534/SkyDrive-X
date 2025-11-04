/**
 * Models for Microsoft Graph JSON batching, used to submit multiple requests in a single round-trip.
 */
package com.lurenjia534.skydrivex.data.model.batch

import com.squareup.moshi.JsonClass
import com.lurenjia534.skydrivex.data.model.driveitem.MoveItemBody

@JsonClass(generateAdapter = true)
data class BatchRequest(
    val requests: List<BatchSubRequest>
)

@JsonClass(generateAdapter = true)
data class BatchSubRequest(
    val id: String,
    val method: String,
    val url: String,
    val headers: Map<String, String>? = null,
    val body: MoveItemBody? = null
)

@JsonClass(generateAdapter = true)
data class BatchResponse(
    val responses: List<BatchSubResponse>
)

@JsonClass(generateAdapter = true)
data class BatchSubResponse(
    val id: String,
    val status: Int,
    val body: Map<String, Any?>? = null
)
