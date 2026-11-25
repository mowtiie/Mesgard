package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.ProfileNoticeDto
import com.mowtiie.mesgard.data.remote.ProfileNoticeRemoteSource
import com.mowtiie.mesgard.domain.repository.ProfileNotice
import com.mowtiie.mesgard.domain.repository.ProfileNoticeKind
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemoteProfileNoticeRepositoryTest {
    private val remote = mockk<ProfileNoticeRemoteSource>()
    private val repository = RemoteProfileNoticeRepository(remote)

    @Test
    fun `a notice the app does not know is kept as another kind`() = runTest {
        coEvery { remote.unseen() } returns listOf(
            ProfileNoticeDto("n1", "username_reset"),
            ProfileNoticeDto("n2", "x"),
            ProfileNoticeDto("n3", "picture_removed"),
            ProfileNoticeDto("n4", "group_picture_removed"),
            ProfileNoticeDto("n5", "group_renamed"),
        )

        val notices = repository.unseen()

        assertEquals(
            listOf(
                ProfileNotice("n1", ProfileNoticeKind.USERNAME_RESET),
                ProfileNotice("n2", ProfileNoticeKind.OTHER),
                ProfileNotice("n3", ProfileNoticeKind.PICTURE_REMOVED),
                ProfileNotice("n4", ProfileNoticeKind.GROUP_PICTURE_REMOVED),
                ProfileNotice("n5", ProfileNoticeKind.GROUP_RENAMED),
            ),
            notices,
        )
    }

    @Test
    fun `no connection means no notice now and the same notice next time`() = runTest {
        coEvery { remote.unseen() } throws IOException("offline")

        assertEquals(emptyList<ProfileNotice>(), repository.unseen())
    }

    @Test
    fun `acknowledging sends the ids, and nothing when there is no notice`() = runTest {
        coEvery { remote.acknowledge(any()) } returns Unit

        repository.acknowledge(listOf(ProfileNotice("n1", ProfileNoticeKind.USERNAME_RESET)))
        repository.acknowledge(emptyList())

        coVerify(exactly = 1) { remote.acknowledge(listOf("n1")) }
    }
}
