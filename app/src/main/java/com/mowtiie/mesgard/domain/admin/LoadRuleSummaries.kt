package com.mowtiie.mesgard.domain.admin

import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.MessagingRules
import com.mowtiie.mesgard.domain.repository.LegalHoldRepository
import com.mowtiie.mesgard.domain.repository.MessagingRulesRepository
import com.mowtiie.mesgard.domain.repository.RetentionRepository
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * What an organization has set, so Administration can show it instead of describing the page. A value is null when
 * the server could not be asked, and the page then falls back to its description.
 */
internal data class RuleSummaries(
    val retention: RetentionSummary? = null,
    val messagingRules: MessagingRules? = null,
    val activeHoldCount: Int? = null,
)

/** The days messages are kept, or [days] null when they are kept without a limit. */
internal data class RetentionSummary(val days: Int?)

internal class LoadRuleSummaries @Inject constructor(
    private val retentionRepository: RetentionRepository,
    private val messagingRulesRepository: MessagingRulesRepository,
    private val legalHoldRepository: LegalHoldRepository,
) {
    suspend operator fun invoke(organizationId: String): RuleSummaries = coroutineScope {
        val retention = async { retentionRepository.retentionDays(organizationId) }
        val rules = async { messagingRulesRepository.rules(organizationId) }
        val holds = async { legalHoldRepository.activeHolds(organizationId) }
        RuleSummaries(
            retention = (retention.await() as? AdminResult.Success)?.let { RetentionSummary(it.value) },
            messagingRules = (rules.await() as? AdminResult.Success)?.value,
            activeHoldCount = (holds.await() as? AdminResult.Success)?.value?.size,
        )
    }
}
