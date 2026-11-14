package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.AdminResult

internal interface AnnouncementRepository {
    /** Makes sure the organization has its announcement conversation, stored on the device, and returns its id. */
    suspend fun openAnnouncements(orgId: String): AdminResult<String>
}
