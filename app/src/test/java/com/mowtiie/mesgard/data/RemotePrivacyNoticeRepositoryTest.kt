package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.PrivacyNoticeDto
import com.mowtiie.mesgard.data.remote.PrivacyNoticeRemoteSource
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemotePrivacyNoticeRepositoryTest {
    private val remote = mockk<PrivacyNoticeRemoteSource>()
    private val repository = RemotePrivacyNoticeRepository(remote)

    @Test
    fun `the notices of the organizations are listed with what is due`() = runTest {
        coEvery { remote.notices() } returns listOf(
            PrivacyNoticeDto("o1", "School", "https://school.example/privacy", null, isAcknowledged = false),
            PrivacyNoticeDto("o2", "Club", null, null, isAcknowledged = true),
        )

        val notices = (repository.notices() as AdminResult.Success).value

        assertEquals(listOf("o1", "o2"), notices.map { it.orgId })
        assertTrue(notices[0].needsAcknowledgment)
        assertFalse(notices[1].needsAcknowledgment)
    }

    @Test
    fun `blank links are sent as no link`() = runTest {
        coEvery { remote.setLinks("o1", any(), any()) } returns Unit

        repository.setLinks("o1", " https://school.example/privacy ", "  ")

        coVerify { remote.setLinks("o1", "https://school.example/privacy", null) }
    }

    @Test
    fun `acknowledging goes to the organization`() = runTest {
        coEvery { remote.acknowledge("o1") } returns Unit

        assertEquals(AdminResult.Success(Unit), repository.acknowledge("o1"))
    }

    @Test
    fun `no connection is reported as such`() = runTest {
        coEvery { remote.notices() } throws IOException("offline")

        assertEquals(AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE), repository.notices())
    }
}
