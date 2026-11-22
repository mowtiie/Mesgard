package com.mowtiie.mesgard.domain.model

/**
 * What the server did with one row of the file. [error] is set when the row was refused, and nothing of it was
 * kept.
 */
internal data class GroupImportRowResult(
    val line: Int,
    val groupCreated: Boolean,
    /** Whether the person of the row was added, was already in the group, or could not be found. */
    val member: ImportedMember?,
    val error: String?,
)

internal enum class ImportedMember {
    ADDED,
    ALREADY_IN_GROUP,
    NOT_FOUND,
}

internal data class GroupImportResult(
    val createdGroups: Int,
    val addedMembers: Int,
    val rows: List<GroupImportRowResult>,
) {
    /** The rows the admin has to look at: refused ones and people who were not found. */
    val rowsToReview: List<GroupImportRowResult>
        get() = rows.filter { it.error != null || it.member == ImportedMember.NOT_FOUND }
}
