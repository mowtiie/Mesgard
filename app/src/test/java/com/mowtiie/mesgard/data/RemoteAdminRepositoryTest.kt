package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.AdminRemoteSource
import com.mowtiie.mesgard.data.remote.CreatedMembersDto
import com.mowtiie.mesgard.data.remote.MemberCreationDto
import com.mowtiie.mesgard.data.remote.MfaResetDto
import com.mowtiie.mesgard.data.remote.PasswordResetDto
import com.mowtiie.mesgard.data.remote.RemovedMemberDto
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.MemberCreation
import com.mowtiie.mesgard.domain.model.NewMember
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.model.RemovedMember
import com.mowtiie.mesgard.domain.model.SkipReason
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemoteAdminRepositoryTest {
    private val remote = mockk<AdminRemoteSource>()
    private val repository = RemoteAdminRepository(remote)

    @Test
    fun `sends the new members with their role names and maps each answer`() = runTest {
        coEvery { remote.createMembers(ORG, any()) } returns CreatedMembersDto(
            listOf(
                MemberCreationDto("ana@school.example", "created", userId = "u1", temporaryPassword = "pw"),
                MemberCreationDto("ben@school.example", "email_taken"),
                MemberCreationDto("x", "invalid", reason = "invalid_email"),
            ),
        )

        val result = repository.createMembers(
            ORG,
            listOf(
                NewMember("ana@school.example", "Ana", OrgRole.STUDENT),
                NewMember("ben@school.example", "Ben", OrgRole.STAFF),
                NewMember("x", "X", OrgRole.ORG_ADMIN),
            ),
        )

        assertEquals(
            AdminResult.Success(
                listOf(
                    MemberCreation.Created("ana@school.example", "u1", "pw"),
                    MemberCreation.Skipped("ben@school.example", SkipReason.EMAIL_TAKEN),
                    MemberCreation.Skipped("x", SkipReason.INVALID_EMAIL),
                ),
            ),
            result,
        )
        coVerify {
            remote.createMembers(
                ORG,
                match { requests -> requests.map { it.role } == listOf("student", "staff", "org_admin") },
            )
        }
    }

    @Test
    fun `reads every status the server can report for a person`() {
        fun status(status: String, reason: String? = null) =
            MemberCreationDto("a", status, reason = reason).toCreation()

        assertEquals(MemberCreation.Skipped("a", SkipReason.DUPLICATE), status("duplicate"))
        assertEquals(MemberCreation.Skipped("a", SkipReason.INVALID_ROLE), status("invalid", "invalid_role"))
        assertEquals(MemberCreation.Skipped("a", SkipReason.INVALID_NAME), status("invalid", "invalid_display_name"))
        assertEquals(MemberCreation.Skipped("a", SkipReason.FAILED), status("failed"))
        assertEquals(MemberCreation.Skipped("a", SkipReason.FAILED), status("created"))
        assertEquals(MemberCreation.Skipped("a", SkipReason.FAILED), status("something new"))
    }

    @Test
    fun `refuses a role that a member cannot have`() = runTest {
        val result = repository.createMembers(ORG, listOf(NewMember("a@b.example", "A", OrgRole.OTHER)))

        assertEquals(AdminResult.Failure(AdminError.UNKNOWN), result)
    }

    @Test
    fun `returns the temporary password of a reset`() = runTest {
        coEvery { remote.resetPassword(ORG, USER) } returns PasswordResetDto("temp")

        assertEquals(AdminResult.Success("temp"), repository.resetPassword(ORG, USER))
    }

    @Test
    fun `resets the two-step verification of a member`() = runTest {
        coEvery { remote.resetMfa(ORG, USER) } returns MfaResetDto(factorsRemoved = 1)

        assertEquals(AdminResult.Success(Unit), repository.resetMfa(ORG, USER))
    }

    @Test
    fun `reports whether the login was disabled when a member is removed`() = runTest {
        coEvery { remote.removeMember(ORG, USER) } returns RemovedMemberDto(removed = true, loginDisabled = true)

        assertEquals(AdminResult.Success(RemovedMember(loginDisabled = true)), repository.removeMember(ORG, USER))
    }

    @Test
    fun `changing a role that no row accepts is not allowed`() = runTest {
        coEvery { remote.changeRole(ORG, USER, "staff") } returns false
        coEvery { remote.changeRole(ORG, USER, "student") } returns true

        assertEquals(AdminResult.Failure(AdminError.NOT_ALLOWED), repository.changeRole(ORG, USER, OrgRole.STAFF))
        assertEquals(AdminResult.Success(Unit), repository.changeRole(ORG, USER, OrgRole.STUDENT))
    }

    @Test
    fun `an unreachable server is a network problem`() = runTest {
        coEvery { remote.resetPassword(ORG, USER) } throws IOException("offline")

        assertEquals(AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE), repository.resetPassword(ORG, USER))
    }

    @Test
    fun `an answer that cannot be read is an unknown problem`() = runTest {
        coEvery { remote.resetPassword(ORG, USER) } coAnswers {
            Json.decodeFromString<PasswordResetDto>("{}")
        }

        assertEquals(AdminResult.Failure(AdminError.UNKNOWN), repository.resetPassword(ORG, USER))
    }

    @Test
    fun `maps the status of a refusal to what the person can do about it`() {
        assertEquals(AdminError.NOT_ALLOWED, adminErrorFor(401))
        assertEquals(AdminError.NOT_ALLOWED, adminErrorFor(403))
        assertEquals(AdminError.NOT_A_MEMBER, adminErrorFor(404))
        assertEquals(AdminError.LAST_ADMIN, adminErrorFor(409))
        assertEquals(AdminError.UNKNOWN, adminErrorFor(400))
        assertEquals(AdminError.UNKNOWN, adminErrorFor(500))
    }

    private companion object {
        const val ORG = "org"
        const val USER = "user"
    }
}
