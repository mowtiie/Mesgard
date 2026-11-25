package com.mowtiie.mesgard.domain.model

/**
 * What the signed in person may do with the picture of a group: [canSet] and [canRemove] come from their role in the
 * group and from the organization policy, and [version] is the picture the group has.
 */
internal data class GroupPictureRules(
    val groupId: String,
    val orgId: String,
    val name: String,
    val canSet: Boolean,
    val canRemove: Boolean,
    val version: Int?,
)
