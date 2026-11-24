package com.mowtiie.mesgard.domain.avatar

import kotlinx.coroutines.flow.Flow

/** The profile picture of a person in one organization. A new picture has a new [version]. */
internal data class AvatarRef(val orgId: String, val userId: String, val version: Int)

/** Where profile pictures come from. A picture is kept on the phone, encrypted, and fetched when it changes. */
internal interface AvatarSource {
    /** The path of the local file of the picture, fetched when needed. It is null when it cannot be had now. */
    suspend fun localPath(ref: AvatarRef): String?

    /** The picture version of a person as the roster on the phone knows it, null when they have none. */
    fun observeRef(userId: String): Flow<AvatarRef?>
}
