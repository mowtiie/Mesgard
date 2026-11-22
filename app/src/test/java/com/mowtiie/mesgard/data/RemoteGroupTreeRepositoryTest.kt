package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.GroupAdminRemoteSource
import com.mowtiie.mesgard.data.remote.GroupRowDto
import com.mowtiie.mesgard.data.remote.GroupTreeRemoteSource
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminGroup
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupKind
import com.mowtiie.mesgard.domain.model.GroupPlacement
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemoteGroupTreeRepositoryTest {
    private val remote = mockk<GroupTreeRemoteSource>()
    private val repository = RemoteGroupTreeRepository(remote)

    @Test
    fun `sends the kind by the name the server knows`() = runTest {
        coEvery { remote.setKind("g1", "department") } returns true

        assertEquals(AdminResult.Success(Unit), repository.changeKind("g1", GroupKind.DEPARTMENT))
        coVerify { remote.setKind("g1", "department") }
    }

    @Test
    fun `moves a group inside another and back to the top`() = runTest {
        coEvery { remote.setParent("g1", "g0") } returns true
        coEvery { remote.setParent("g1", null) } returns true

        assertEquals(AdminResult.Success(Unit), repository.moveGroup("g1", "g0"))
        assertEquals(AdminResult.Success(Unit), repository.moveGroup("g1", null))
    }

    @Test
    fun `a change that touches no group is not allowed`() = runTest {
        coEvery { remote.setKind("g1", "club") } returns false

        assertEquals(AdminResult.Failure(AdminError.NOT_ALLOWED), repository.changeKind("g1", GroupKind.CLUB))
    }

    @Test
    fun `reads the kind and the parent of every group and creates a group in its place`() = runTest {
        val groups = mockk<GroupAdminRemoteSource>()
        val admin = RemoteGroupAdminRepository(groups)
        coEvery { groups.listGroups("org") } returns
            listOf(GroupRowDto("g2", "STEM", kind = "section", parentId = "g1"))
        coEvery { groups.createGroup("org", "Robotics", "everyone", false, any()) } returns "g3"

        assertEquals(
            AdminResult.Success(listOf(AdminGroup("g2", "STEM", 0, GroupKind.SECTION, "g1"))),
            admin.listGroups("org"),
        )
        assertEquals(
            AdminResult.Success("g3"),
            admin.createGroup("org", "Robotics", placement = GroupPlacement(GroupKind.CLUB, "g1")),
        )
        coVerify {
            groups.createGroup(
                "org",
                "Robotics",
                "everyone",
                false,
                match { it.kind == "club" && it.parentId == "g1" },
            )
        }
    }
}
