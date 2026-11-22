package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.GroupAnnouncementRemoteSource
import com.mowtiie.mesgard.domain.model.AdminResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemoteGroupAnnouncementRepositoryTest {
    private val remote = mockk<GroupAnnouncementRemoteSource>()
    private val repository = RemoteGroupAnnouncementRepository(remote)

    @Test
    fun `sends the trimmed text with the message id and returns how many conversations it reached`() = runTest {
        coEvery { remote.sendToGroupTree("g1", "m1", "Exams start Monday") } returns 3

        assertEquals(AdminResult.Success(3), repository.send("g1", "m1", "  Exams start Monday \n"))
        coVerify { remote.sendToGroupTree("g1", "m1", "Exams start Monday") }
    }
}
