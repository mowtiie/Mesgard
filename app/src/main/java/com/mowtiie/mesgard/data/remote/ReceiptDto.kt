package com.mowtiie.mesgard.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ReceiptDto(
    @SerialName("user_id") val userId: String,
    @SerialName("read_at") val readAt: String,
    val profiles: ReaderProfileDto? = null,
)

@Serializable
internal data class ReaderProfileDto(@SerialName("display_name") val displayName: String)
