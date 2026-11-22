package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.GroupCreationDto
import com.mowtiie.mesgard.data.remote.GroupCreationRemoteSource
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupCreationPolicy
import com.mowtiie.mesgard.domain.model.GroupCreationRules
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemoteGroupCreationRepositoryTest {
    private val remote = mockk<GroupCreationRemoteSource>()
    private val repository = RemoteGroupCreationRepository(remote)

    @Test
    fun `reads who may create groups and how many the person made`() = runTest {
        coEvery { remote.rules("org") } returns GroupCreationDto("everyone", canCreate = true, created = 2, limit = 5)

        assertEquals(
            AdminResult.Success(
                GroupCreationRules(GroupCreationPolicy.EVERYONE, canCreate = true, created = 2, limit = 5),
            ),
            repository.rules("org"),
        )
    }

    @Test
    fun `an unknown policy is read as the strictest`() = runTest {
        coEvery { remote.rules("org") } returns GroupCreationDto("future", canCreate = false, created = 0, limit = 5)

        assertEquals(GroupCreationPolicy.ADMINS_ONLY, (repository.rules("org") as AdminResult.Success).value.policy)
    }

    @Test
    fun `sets the policy by the name the server knows`() = runTest {
        coEvery { remote.setPolicy("org", "staff") } returns Unit

        assertEquals(AdminResult.Success(Unit), repository.setPolicy("org", GroupCreationPolicy.STAFF))
        coVerify { remote.setPolicy("org", "staff") }
    }
}
