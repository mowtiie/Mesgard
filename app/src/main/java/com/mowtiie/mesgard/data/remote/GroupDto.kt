package com.mowtiie.mesgard.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GroupDto(val id: String, @SerialName("org_id") val orgId: String, val name: String)

@Serializable
internal data class MembershipDto(@SerialName("org_id") val orgId: String, val role: String)
