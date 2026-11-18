package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.MentionRemoteSource
import com.mowtiie.mesgard.data.remote.RemoteOutcome
import com.mowtiie.mesgard.data.remote.attemptRemote
import com.mowtiie.mesgard.domain.model.MentionCandidate
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.MENTION_SUGGESTION_LIMIT
import com.mowtiie.mesgard.domain.repository.MentionRepository
import javax.inject.Inject

private const val TAG = "MentionRepository"

internal class RemoteMentionRepository @Inject constructor(
    private val remote: MentionRemoteSource,
    private val authRepository: AuthRepository,
) : MentionRepository {
    override suspend fun search(conversationId: String, query: String): List<MentionCandidate> {
        val me = authRepository.currentUserId
        // One more than shown, because the current user can be among the results and is removed.
        val outcome =
            attemptRemote(TAG) { remote.searchParticipants(conversationId, query, MENTION_SUGGESTION_LIMIT + 1) }
        return when (outcome) {
            is RemoteOutcome.Success ->
                outcome.value
                    .filter { it.userId != me }
                    .map { MentionCandidate(it.userId, it.member.profiles.displayName) }
                    .take(MENTION_SUGGESTION_LIMIT)
            is RemoteOutcome.Failure -> emptyList()
        }
    }
}
