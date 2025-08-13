package com.lurenjia534.skydrivex.data.model.drive

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DriveDto(
    val id: String?,
    val driveType: String?,
    val quota: Quota?,
)

@JsonClass(generateAdapter = true)
data class Quota(
    val total: Long?,
    val used: Long?,
    val remaining: Long?,
    val deleted: Long?,
    val state: String?,
    val storagePlanInformation: StoragePlanInformation?,
)

@JsonClass(generateAdapter = true)
data class StoragePlanInformation(
    val upgradeAvailable: Boolean?,
)
