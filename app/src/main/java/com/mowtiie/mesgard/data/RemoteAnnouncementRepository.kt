package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.AnnouncementRemoteSource
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.AnnouncementRepository
import com.mowtiie.mesgard.domain.repository.ConversationRepository
import javax.inject.Inject

internal class RemoteAnnouncementRepository @Inject constructor(
    private val remote: AnnouncementRemoteSource,
    private val conversationRepository: ConversationRepository,
) : AnnouncementRepository {
    override suspend fun openAnnouncements(orgId: String): AdminResult<String> {
        val created = attemptAdmin { remote.createAnnouncementConversation(orgId) }
        if (created !is AdminResult.Success) return created
        // The conversation screen reads from the device, so the conversation must be stored before it opens.
        return when (val refreshed = conversationRepository.refresh()) {
            RefreshResult.Success -> created
            is RefreshResult.Failure -> AdminResult.Failure(refreshed.error.toAdminError())
        }
    }
}

private fun RefreshError.toAdminError() = when (this) {
    RefreshError.NETWORK_UNAVAILABLE -> AdminError.NETWORK_UNAVAILABLE
    RefreshError.UNKNOWN -> AdminError.UNKNOWN
}
