package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.GroupImportRemoteSource
import com.mowtiie.mesgard.data.remote.GroupImportResultDto
import com.mowtiie.mesgard.data.remote.GroupImportRowDto
import com.mowtiie.mesgard.data.remote.GroupImportRowResultDto
import com.mowtiie.mesgard.domain.admin.GroupImportRow
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupImportResult
import com.mowtiie.mesgard.domain.model.GroupImportRowResult
import com.mowtiie.mesgard.domain.model.GroupKind
import com.mowtiie.mesgard.domain.model.GroupRole
import com.mowtiie.mesgard.domain.model.ImportedMember
import com.mowtiie.mesgard.domain.repository.GroupImportRepository
import javax.inject.Inject

internal class RemoteGroupImportRepository @Inject constructor(private val remote: GroupImportRemoteSource) :
    GroupImportRepository {
    override suspend fun import(orgId: String, rows: List<GroupImportRow>): AdminResult<GroupImportResult> =
        attemptAdmin { remote.import(orgId, rows.map { it.toDto() }).toResult() }
}

private fun GroupImportRow.toDto() = GroupImportRowDto(
    line = line,
    group = group,
    parent = parent,
    kind = (kind ?: GroupKind.OTHER).toWireName(),
    email = email,
    role = (role ?: GroupRole.MEMBER).name.lowercase(),
)

private fun GroupImportResultDto.toResult() = GroupImportResult(createdGroups, addedMembers, rows.map { it.toResult() })

private fun GroupImportRowResultDto.toResult() = GroupImportRowResult(
    line = line,
    groupCreated = group == "created",
    member = when (member) {
        "added" -> ImportedMember.ADDED
        "already" -> ImportedMember.ALREADY_IN_GROUP
        "not_found" -> ImportedMember.NOT_FOUND
        else -> null
    },
    error = error,
)
