package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.avatar.CropSquare
import com.mowtiie.mesgard.domain.avatar.PreparedPhoto
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupPictureRules

/** The picture of a group: administrators, owners and moderators set it. It needs a connection. */
internal interface GroupPictureRepository {
    suspend fun rules(groupId: String): AdminResult<GroupPictureRules>

    /** Cuts [crop] from the photo, uploads it as the next version and makes it the picture of the group. */
    suspend fun set(groupId: String, photo: PreparedPhoto, crop: CropSquare): AdminResult<Unit>

    suspend fun remove(groupId: String): AdminResult<Unit>
}
