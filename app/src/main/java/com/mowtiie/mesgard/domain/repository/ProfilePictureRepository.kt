package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.avatar.CropSquare
import com.mowtiie.mesgard.domain.avatar.PreparedPhoto
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.ProfilePicturePolicy
import com.mowtiie.mesgard.domain.model.ProfilePictureRules

/** The profile picture of the signed in person and who may have one. It needs a connection. */
internal interface ProfilePictureRepository {
    suspend fun rules(orgId: String): AdminResult<ProfilePictureRules>

    /** Cuts [crop] from the photo, uploads it as the next version and makes it the picture. */
    suspend fun set(orgId: String, photo: PreparedPhoto, crop: CropSquare): AdminResult<Unit>

    suspend fun remove(orgId: String): AdminResult<Unit>

    suspend fun setPolicy(orgId: String, policy: ProfilePicturePolicy): AdminResult<Unit>
}
