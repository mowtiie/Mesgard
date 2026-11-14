package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.CountDto
import com.mowtiie.mesgard.data.remote.GroupAdminRemoteSource
import com.mowtiie.mesgard.data.remote.GroupMemberRowDto
import com.mowtiie.mesgard.data.remote.GroupRowDto
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminGroup
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupDetail
import com.mowtiie.mesgard.domain.model.GroupMember
import com.mowtiie.mesgard.domain.model.GroupRole
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
class RemoteGroupAdminRepositoryTest {
    private val remote = mockk<GroupAdminRemoteSource>()
    private val repository = RemoteGroupAdminRepository(remote)

    @Test
    fun `lists the groups with the number of their members`() = runTest {
        coEvery { remote.listGroups(ORG) } returns listOf(
            GroupRowDto("g1", "Grade 10 A", listOf(CountDto(28))),
            GroupRowDto("g2", "Science Department"),
        )

        assertEquals(
            AdminResult.Success(listOf(AdminGroup("g1", "Grade 10 A", 28), AdminGroup("g2", "Science Department", 0))),
            repository.listGroups(ORG),
        )
    }

    @Test
    fun `reads a group with its name and the roles of its members`() = runTest {
        coEvery { remote.fetchGroupName("g1") } returns "Grade 10 A"
        coEvery { remote.fetchGroupMembers("g1") } returns listOf(
            GroupMemberRowDto("u1", "moderator"),
            GroupMemberRowDto("u2", "member"),
        )

        assertEquals(
            AdminResult.Success(
                GroupDetail(
                    "g1",
                    "Grade 10 A",
                    listOf(GroupMember("u1", GroupRole.MODERATOR), GroupMember("u2", GroupRole.MEMBER)),
                ),
            ),
            repository.fetchGroup("g1"),
        )
    }

    @Test
    fun `a group that cannot be seen is not allowed`() = runTest {
        coEvery { remote.fetchGroupName("g1") } returns null

        assertEquals(AdminResult.Failure(AdminError.NOT_ALLOWED), repository.fetchGroup("g1"))
    }

    @Test
    fun `creates and renames a group with a trimmed name`() = runTest {
        coEvery { remote.createGroup(ORG, "Chess Club") } returns "g9"
        coEvery { remote.renameGroup("g9", "Chess Society") } returns true

        assertEquals(AdminResult.Success("g9"), repository.createGroup(ORG, "  Chess Club "))
        assertEquals(AdminResult.Success(Unit), repository.renameGroup("g9", " Chess Society  "))
    }

    @Test
    fun `a write that changes no row is not allowed`() = runTest {
        coEvery { remote.renameGroup("g1", "New") } returns false
        coEvery { remote.deleteGroup("g1") } returns false
        coEvery { remote.removeMember("g1", "u1") } returns false
        coEvery { remote.changeMemberRole("g1", "u1", "moderator") } returns false

        val denied = AdminResult.Failure(AdminError.NOT_ALLOWED)
        assertEquals(denied, repository.renameGroup("g1", "New"))
        assertEquals(denied, repository.deleteGroup("g1"))
        assertEquals(denied, repository.removeMember("g1", "u1"))
        assertEquals(denied, repository.changeMemberRole("g1", "u1", GroupRole.MODERATOR))
    }

    @Test
    fun `adds members and sends the role names the server knows`() = runTest {
        coEvery { remote.addMembers(ORG, "g1", listOf("u1", "u2")) } returns Unit
        coEvery { remote.changeMemberRole("g1", "u1", "member") } returns true

        assertEquals(AdminResult.Success(Unit), repository.addMembers(ORG, "g1", listOf("u1", "u2")))
        assertEquals(AdminResult.Success(Unit), repository.changeMemberRole("g1", "u1", GroupRole.MEMBER))
        coVerify { remote.changeMemberRole("g1", "u1", "member") }
    }

    @Test
    fun `an unreachable server is a network problem`() = runTest {
        coEvery { remote.listGroups(ORG) } throws IOException("offline")

        assertEquals(AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE), repository.listGroups(ORG))
    }

    private companion object {
        const val ORG = "org"
    }
}
