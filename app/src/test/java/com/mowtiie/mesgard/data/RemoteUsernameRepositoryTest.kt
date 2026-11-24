package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.UsernameRemoteSource
import com.mowtiie.mesgard.data.remote.UsernameRulesDto
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.UsernamePolicy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemoteUsernameRepositoryTest {
    private val remote = mockk<UsernameRemoteSource>()
    private val repository = RemoteUsernameRepository(remote)

    @Test
    fun `the rules carry the username, the policy and when the wait ends`() = runTest {
        coEvery { remote.rules("org1") } returns
            UsernameRulesDto("staff_and_admins", "ana.cruz", true, "2026-12-20T08:00:00+00:00")

        val rules = (repository.rules("org1") as AdminResult.Success).value

        assertEquals(UsernamePolicy.STAFF_AND_ADMINS, rules.policy)
        assertEquals("ana.cruz", rules.username)
        assertNotNull(rules.nextChangeAtEpochMillis)
    }

    @Test
    fun `a policy the app does not know counts as the strictest`() = runTest {
        coEvery { remote.rules("org1") } returns UsernameRulesDto("sometimes", "ana.cruz", false)

        val rules = (repository.rules("org1") as AdminResult.Success).value

        assertEquals(UsernamePolicy.NOBODY, rules.policy)
        assertNull(rules.nextChangeAtEpochMillis)
    }

    @Test
    fun `a change is sent trimmed and in lowercase`() = runTest {
        coEvery { remote.change(any(), any()) } returns Unit

        repository.change("org1", "  Ana.Cruz ")

        coVerify { remote.change("org1", "ana.cruz") }
    }

    @Test
    fun `no connection is reported as such`() = runTest {
        coEvery { remote.change(any(), any()) } throws IOException("offline")

        val result = repository.change("org1", "ana.cruz")

        assertEquals(AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE), result)
    }

    @Test
    fun `the words of the server decide which username error it is`() {
        assertEquals(AdminError.USERNAME_INVALID, adminErrorFor(400, message = "Invalid username"))
        assertEquals(AdminError.USERNAME_RESERVED, adminErrorFor(400, message = "Username is reserved"))
        assertEquals(
            AdminError.USERNAME_TOO_SOON,
            adminErrorFor(400, message = "A username can change once every 30 days"),
        )
        assertEquals(AdminError.USERNAME_TAKEN, adminErrorFor(409, AdminError.USERNAME_TAKEN, "Username is taken"))
    }
}
