package com.mowtiie.mesgard.domain.repository

internal enum class ProfileNoticeKind {
    USERNAME_RESET,
    PICTURE_REMOVED,
    GROUP_PICTURE_REMOVED,
    GROUP_RENAMED,
    OTHER,
}

internal data class ProfileNotice(val id: String, val kind: ProfileNoticeKind)

/** One time notices for the signed in person, for example that a reviewer reset their username. */
internal interface ProfileNoticeRepository {
    /** The notices not shown yet. It is empty when the server cannot be asked, and they come again next time. */
    suspend fun unseen(): List<ProfileNotice>

    suspend fun acknowledge(notices: Collection<ProfileNotice>)
}
