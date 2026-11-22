package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.admin.GroupImportRow
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupImportResult

/**
 * Creates the groups of a file and puts people in them. A group is found by its name inside its parent, so the same
 * file can be sent again. It needs a connection and only organization admins can do it.
 */
internal interface GroupImportRepository {
    suspend fun import(orgId: String, rows: List<GroupImportRow>): AdminResult<GroupImportResult>
}
