package com.mowtiie.mesgard.domain.admin

import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupCreationPolicy
import com.mowtiie.mesgard.domain.model.GroupCreationRules
import com.mowtiie.mesgard.domain.model.MessagingRules
import com.mowtiie.mesgard.domain.repository.GroupCreationRepository
import com.mowtiie.mesgard.domain.repository.LegalHoldRepository
import com.mowtiie.mesgard.domain.repository.MessagingRulesRepository
import com.mowtiie.mesgard.domain.repository.RetentionRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoadRuleSummariesTest {
    private val retention = mockk<RetentionRepository>()
    private val messagingRules = mockk<MessagingRulesRepository>()
    private val legalHolds = mockk<LegalHoldRepository>()
    private val groupCreation = mockk<GroupCreationRepository>()
    private val load = LoadRuleSummaries(retention, messagingRules, legalHolds, groupCreation)

    @Test
    fun `collects what the organization has set`() = runTest {
        val rules = MessagingRules(blockStaffStudentDirectMessages = true)
        coEvery { retention.retentionDays("org") } returns AdminResult.Success(730)
        coEvery { messagingRules.rules("org") } returns AdminResult.Success(rules)
        coEvery { legalHolds.activeHolds("org") } returns AdminResult.Success(emptyList())
        coEvery { groupCreation.rules("org") } returns
            AdminResult.Success(GroupCreationRules(GroupCreationPolicy.STAFF, canCreate = true, created = 0, limit = 5))

        val summaries = load("org")

        assertEquals(RetentionSummary(730), summaries.retention)
        assertEquals(rules, summaries.messagingRules)
        assertEquals(0, summaries.activeHoldCount)
        assertEquals(GroupCreationPolicy.STAFF, summaries.groupCreation)
    }

    @Test
    fun `no limit on messages is not the same as not knowing`() = runTest {
        coEvery { retention.retentionDays("org") } returns AdminResult.Success(null)
        coEvery { messagingRules.rules("org") } returns AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE)
        coEvery { legalHolds.activeHolds("org") } returns AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE)
        coEvery { groupCreation.rules("org") } returns AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE)

        val summaries = load("org")

        assertEquals(RetentionSummary(null), summaries.retention)
        assertNull(summaries.messagingRules)
        assertNull(summaries.activeHoldCount)
        assertNull(summaries.groupCreation)
    }
}
