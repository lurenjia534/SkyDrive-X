package com.lurenjia534.skydrivex.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserDto(
    val id: String,
    val businessPhones: List<String>?,
    val displayName: String?,
    val givenName: String?,
    val jobTitle: String?,
    val mail: String?,
    val mobilePhone: String?,
    val officeLocation: String?,
    val preferredLanguage: String?,
    val surname: String?,
    val userPrincipalName: String?
)
