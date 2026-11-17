package com.mowtiie.mesgard.data

import android.util.Log
import com.mowtiie.mesgard.data.remote.AdminRemoteSource
import com.mowtiie.mesgard.data.remote.MemberCreationDto
import com.mowtiie.mesgard.data.remote.NewMemberRequest
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.MemberCreation
import com.mowtiie.mesgard.domain.model.NewMember
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.model.RemovedMember
import com.mowtiie.mesgard.domain.model.SkipReason
import com.mowtiie.mesgard.domain.repository.AdminRepository
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import java.io.IOException
import javax.inject.Inject

private const val TAG = "AdminRepository"
private const val HTTP_BAD_REQUEST = 400
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val HTTP_NOT_FOUND = 404
private const val HTTP_CONFLICT = 409

/** Administration is never cached: it only works with a connection, and every answer comes from the server. */
internal class RemoteAdminRepository @Inject constructor(private val remote: AdminRemoteSource) : AdminRepository {
    override suspend fun createMembers(orgId: String, members: List<NewMember>): AdminResult<List<MemberCreation>> =
        attemptAdmin {
            val requests = members.map { NewMemberRequest(it.email, it.displayName, it.role.wireName()) }
            remote.createMembers(orgId, requests).results.map { it.toCreation() }
        }

    override suspend fun resetPassword(orgId: String, userId: String): AdminResult<String> =
        attemptAdmin { remote.resetPassword(orgId, userId).temporaryPassword }

    override suspend fun removeMember(orgId: String, userId: String): AdminResult<RemovedMember> = attemptAdmin {
        RemovedMember(loginDisabled = remote.removeMember(orgId, userId).loginDisabled)
    }

    override suspend fun resetMfa(orgId: String, userId: String): AdminResult<Unit> = attemptAdmin<Unit> {
        remote.resetMfa(orgId, userId)
    }

    override suspend fun changeRole(orgId: String, userId: String, role: OrgRole): AdminResult<Unit> = attemptAdmin {
        check(remote.changeRole(orgId, userId, role.wireName())) { "No member was changed" }
    }
}

internal suspend fun <T> attemptAdmin(block: suspend () -> T): AdminResult<T> = try {
    AdminResult.Success(block())
} catch (exception: HttpRequestException) {
    Log.i(TAG, "The server could not be reached", exception)
    AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE)
} catch (exception: IOException) {
    Log.i(TAG, "The server could not be reached", exception)
    AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE)
} catch (exception: RestException) {
    Log.w(TAG, "The server refused the request", exception)
    AdminResult.Failure(adminErrorFor(exception.statusCode))
} catch (exception: IllegalArgumentException) {
    Log.w(TAG, "The request or the answer is not valid", exception)
    AdminResult.Failure(AdminError.UNKNOWN)
} catch (exception: IllegalStateException) {
    Log.w(TAG, "The request changed nothing", exception)
    AdminResult.Failure(AdminError.NOT_ALLOWED)
}

internal fun adminErrorFor(statusCode: Int): AdminError = when (statusCode) {
    HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> AdminError.NOT_ALLOWED
    HTTP_NOT_FOUND -> AdminError.NOT_A_MEMBER
    HTTP_CONFLICT -> AdminError.LAST_ADMIN
    HTTP_BAD_REQUEST -> AdminError.UNKNOWN
    else -> AdminError.UNKNOWN
}

private fun OrgRole.wireName(): String = when (this) {
    OrgRole.ORG_ADMIN -> "org_admin"
    OrgRole.STAFF -> "staff"
    OrgRole.STUDENT -> "student"
    OrgRole.OTHER -> throw IllegalArgumentException("A member needs the role admin, staff or student")
}

internal fun MemberCreationDto.toCreation(): MemberCreation = when (status) {
    "created" -> if (userId != null && temporaryPassword != null) {
        MemberCreation.Created(email, userId, temporaryPassword)
    } else {
        MemberCreation.Skipped(email, SkipReason.FAILED)
    }
    "email_taken" -> MemberCreation.Skipped(email, SkipReason.EMAIL_TAKEN)
    "duplicate" -> MemberCreation.Skipped(email, SkipReason.DUPLICATE)
    "invalid" -> MemberCreation.Skipped(email, invalidReason(reason))
    else -> MemberCreation.Skipped(email, SkipReason.FAILED)
}

private fun invalidReason(reason: String?): SkipReason = when (reason) {
    "invalid_email" -> SkipReason.INVALID_EMAIL
    "invalid_role" -> SkipReason.INVALID_ROLE
    "invalid_display_name" -> SkipReason.INVALID_NAME
    else -> SkipReason.FAILED
}
