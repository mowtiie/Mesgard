package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.GroupImportRemoteSource
import com.mowtiie.mesgard.data.remote.GroupImportResultDto
import com.mowtiie.mesgard.data.remote.GroupImportRowResultDto
import com.mowtiie.mesgard.domain.admin.GroupImportRow
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupImportResult
import com.mowtiie.mesgard.domain.model.GroupImportRowResult
import com.mowtiie.mesgard.domain.model.GroupKind
import com.mowtiie.mesgard.domain.model.GroupRole
import com.mowtiie.mesgard.domain.model.ImportedMember
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemoteGroupImportRepositoryTest {
    private val remote = mockk<GroupImportRemoteSource>()
    private val repository = RemoteGroupImportRepository(remote)

    @Test
    fun `sends the rows in the words of the server and reads what it did with each`() = runTest {
        coEvery { remote.import("org", any()) } returns GroupImportResultDto(
            createdGroups = 1,
            addedMembers = 1,
            rows = listOf(
                GroupImportRowResultDto(2, group = "created", member = "added"),
                GroupImportRowResultDto(3, member = "not_found"),
                GroupImportRowResultDto(4, error = "The role must be owner, moderator or member"),
            ),
        )
        val row = GroupImportRow(2, "STEM", "Grade 11", GroupKind.SECTION, "ana@school.example", GroupRole.OWNER, null)

        val result = repository.import("org", listOf(row))

        coVerify {
            remote.import(
                "org",
                match {
                    it.single().kind == "section" && it.single().role == "owner" && it.single().parent == "Grade 11"
                },
            )
        }
        assertEquals(
            AdminResult.Success(
                GroupImportResult(
                    1,
                    1,
                    listOf(
                        GroupImportRowResult(2, groupCreated = true, member = ImportedMember.ADDED, error = null),
                        GroupImportRowResult(3, groupCreated = false, member = ImportedMember.NOT_FOUND, error = null),
                        GroupImportRowResult(4, false, null, "The role must be owner, moderator or member"),
                    ),
                ),
            ),
            result,
        )
        assertEquals(2, (result as AdminResult.Success).value.rowsToReview.size)
    }
}
