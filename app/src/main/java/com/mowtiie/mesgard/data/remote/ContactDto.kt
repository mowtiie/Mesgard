package com.mowtiie.mesgard.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ContactDto(
    @SerialName("org_id") val orgId: String,
    @SerialName("user_id") val userId: String,
    val role: String,
    val username: String = "",
    @SerialName("avatar_version") val avatarVersion: Int? = null,
    val user: SenderDto,
)
