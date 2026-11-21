package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.MessagingRulesDto
import com.mowtiie.mesgard.data.remote.MessagingRulesRemoteSource
import com.mowtiie.mesgard.data.remote.SetMessagingRulesRequest
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.MessagingRules
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteMessagingRulesRepositoryTest {
    private val remote = mockk<MessagingRulesRemoteSource>()
    private val repository = RemoteMessagingRulesRepository(remote)

    @Test
    fun `times come from the server as seconds and are read as a time of day`() = runTest {
        coEvery { remote.rules("org") } returns MessagingRulesDto(true, "21:00:00", "06:30:00", "Asia/Manila")

        val rules = (repository.rules("org") as AdminResult.Success).value

        assertEquals(MessagingRules(true, LocalTime.of(21, 0), LocalTime.of(6, 30), "Asia/Manila"), rules)
    }

    @Test
    fun `no quiet hours stay none`() = runTest {
        coEvery { remote.rules("org") } returns MessagingRulesDto(false, null, null, "Asia/Manila")

        val rules = (repository.rules("org") as AdminResult.Success).value

        assertEquals(false, rules.hasQuietHours)
    }

    @Test
    fun `saving sends hours and minutes`() = runTest {
        coEvery { remote.setRules(any()) } returns Unit

        repository.setRules("org", MessagingRules(true, LocalTime.of(21, 0), LocalTime.of(6, 5), "Asia/Manila"))

        coVerify { remote.setRules(SetMessagingRulesRequest("org", true, "21:00", "06:05", "Asia/Manila")) }
    }

    @Test
    fun `saving without quiet hours sends no times`() = runTest {
        coEvery { remote.setRules(any()) } returns Unit

        repository.setRules("org", MessagingRules(blockStaffStudentDirectMessages = true))

        coVerify { remote.setRules(SetMessagingRulesRequest("org", true, null, null, "Asia/Manila")) }
    }
}
