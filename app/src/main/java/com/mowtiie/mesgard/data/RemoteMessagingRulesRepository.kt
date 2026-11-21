package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.MessagingRulesDto
import com.mowtiie.mesgard.data.remote.MessagingRulesRemoteSource
import com.mowtiie.mesgard.data.remote.SetMessagingRulesRequest
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.MessagingRules
import com.mowtiie.mesgard.domain.repository.MessagingRulesRepository
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private val ServerTime = DateTimeFormatter.ofPattern("HH:mm")

internal class RemoteMessagingRulesRepository @Inject constructor(private val remote: MessagingRulesRemoteSource) :
    MessagingRulesRepository {
    override suspend fun rules(orgId: String): AdminResult<MessagingRules> =
        attemptAdmin { remote.rules(orgId).toRules() }

    override suspend fun setRules(orgId: String, rules: MessagingRules): AdminResult<Unit> = attemptAdmin<Unit> {
        remote.setRules(
            SetMessagingRulesRequest(
                orgId = orgId,
                blockStaffStudent = rules.blockStaffStudentDirectMessages,
                quietStart = rules.quietStart?.format(ServerTime),
                quietEnd = rules.quietEnd?.format(ServerTime),
                timeZone = rules.timeZone,
            ),
        )
    }
}

private fun MessagingRulesDto.toRules() = MessagingRules(
    blockStaffStudentDirectMessages = blockStaffStudentDirectMessages,
    quietStart = quietHoursStart?.let(LocalTime::parse),
    quietEnd = quietHoursEnd?.let(LocalTime::parse),
    timeZone = quietHoursTimeZone,
)
