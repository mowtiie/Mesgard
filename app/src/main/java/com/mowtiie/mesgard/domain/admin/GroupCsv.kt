package com.mowtiie.mesgard.domain.admin

import com.mowtiie.mesgard.domain.model.GroupKind
import com.mowtiie.mesgard.domain.model.GroupRole

private const val MAX_GROUP_NAME_LENGTH = 200
private const val MAX_EMAIL_LENGTH = 254
private const val BYTE_ORDER_MARK_CODE = 0xFEFF
private val EmailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
private val GROUP_HEADERS = setOf("group", "group name", "section", "class", "name")
private val PARENT_HEADERS = setOf("parent", "parent group", "inside", "under")
private val KIND_HEADERS = setOf("kind", "type")
private val EMAIL_HEADERS = setOf("email", "e-mail", "email address", "emailaddress", "member")
private val ROLE_HEADERS = setOf("role", "group role")

internal const val GROUP_TEMPLATE = "group,parent,kind,email,role\n" +
    "Grade 11,,class,,\n" +
    "STEM,Grade 11,section,ana.cruz@school.example,member\n" +
    "STEM,Grade 11,section,ben.reyes@school.example,owner\n"

internal enum class GroupRowProblem {
    NO_GROUP_NAME,
    GROUP_NAME_TOO_LONG,
    UNKNOWN_KIND,
    INVALID_EMAIL,
    UNKNOWN_ROLE,
}

/** One row of the file: a group, where it sits and what kind it is, and optionally one person in it. */
internal data class GroupImportRow(
    val line: Int,
    val group: String,
    val parent: String?,
    val kind: GroupKind?,
    val email: String?,
    val role: GroupRole?,
    val problem: GroupRowProblem?,
)

internal enum class GroupFileProblem {
    EMPTY,
    NO_GROUP_COLUMN,
    TOO_MANY_ROWS,
    UNREADABLE,
}

internal sealed interface GroupParse {
    data class Parsed(val rows: List<GroupImportRow>) : GroupParse

    data class Rejected(val problem: GroupFileProblem) : GroupParse
}

/**
 * Reads a file of groups with a header row. Only the group column is required. A row with an email puts that person in
 * the group, so a group with many people takes many rows. The server finds a group by its name inside its parent, so a
 * parent has to be named the same way each time. A missing kind means other and a missing role means member.
 */
internal fun parseGroupFile(text: String): GroupParse {
    val rows = readCsvRows(text.removePrefix(BYTE_ORDER_MARK_CODE.toChar().toString()))
    val header = rows.firstOrNull()?.cells.orEmpty().map { it.trim().lowercase() }
    val body = rows.drop(1)
    val columns = GroupColumns(
        group = header.indexOfFirst { it in GROUP_HEADERS },
        parent = header.indexOfFirst { it in PARENT_HEADERS },
        kind = header.indexOfFirst { it in KIND_HEADERS },
        email = header.indexOfFirst { it in EMAIL_HEADERS },
        role = header.indexOfFirst { it in ROLE_HEADERS },
    )
    val fileProblem = when {
        header.isEmpty() -> GroupFileProblem.EMPTY
        columns.group < 0 -> GroupFileProblem.NO_GROUP_COLUMN
        body.isEmpty() -> GroupFileProblem.EMPTY
        body.size > MAX_ROSTER_ROWS -> GroupFileProblem.TOO_MANY_ROWS
        else -> null
    }
    return if (fileProblem == null) {
        GroupParse.Parsed(body.map { rowOf(it, columns) })
    } else {
        GroupParse.Rejected(fileProblem)
    }
}

private class GroupColumns(val group: Int, val parent: Int, val kind: Int, val email: Int, val role: Int)

private fun rowOf(row: CsvRow, columns: GroupColumns): GroupImportRow {
    val group = row.cell(columns.group)
    val kind = kindOf(row.cell(columns.kind))
    val email = row.cell(columns.email).lowercase().ifEmpty { null }
    val role = roleOf(row.cell(columns.role))
    val problem = when {
        group.isEmpty() -> GroupRowProblem.NO_GROUP_NAME
        group.length > MAX_GROUP_NAME_LENGTH -> GroupRowProblem.GROUP_NAME_TOO_LONG
        kind == null -> GroupRowProblem.UNKNOWN_KIND
        email != null && (email.length > MAX_EMAIL_LENGTH || !EmailPattern.matches(email)) ->
            GroupRowProblem.INVALID_EMAIL
        role == null -> GroupRowProblem.UNKNOWN_ROLE
        else -> null
    }
    return GroupImportRow(row.line, group, row.cell(columns.parent).ifEmpty { null }, kind, email, role, problem)
}

/** The kind named in a file, other when the cell is empty, or null when it is not a kind Mesgard knows. */
private fun kindOf(cell: String): GroupKind? = when (cell.trim().lowercase()) {
    "" -> GroupKind.OTHER
    "section" -> GroupKind.SECTION
    "class", "grade", "grade level" -> GroupKind.CLASS
    "department", "dept" -> GroupKind.DEPARTMENT
    "club", "org", "organization" -> GroupKind.CLUB
    "other", "group" -> GroupKind.OTHER
    else -> null
}

/** The role named in a file, member when the cell is empty, or null when it is not a role a group has. */
private fun roleOf(cell: String): GroupRole? = when (cell.trim().lowercase()) {
    "", "member", "members", "student", "pupil" -> GroupRole.MEMBER
    "owner", "adviser", "advisor", "teacher", "head" -> GroupRole.OWNER
    "moderator", "officer" -> GroupRole.MODERATOR
    else -> null
}
