package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.AdminError

internal const val EXPORT_PAGE_SIZE = 200

/** The file an export was written to. It sits in the cache and is removed when the person signs out. */
internal data class ExportFile(val path: String, val fileName: String)

internal sealed interface ExportResult {
    data class Ready(val file: ExportFile) : ExportResult

    data class Failure(val error: AdminError) : ExportResult
}

/**
 * Exports what the organization holds about one person, for an access request. Only an organization admin can ask, it
 * needs a connection, and every export is recorded in the audit log.
 */
internal interface DataExportRepository {
    suspend fun export(orgId: String, userId: String, personName: String): ExportResult
}
