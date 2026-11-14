package com.mowtiie.mesgard.domain.admin

import com.mowtiie.mesgard.domain.model.OrgRole

internal const val MAX_ROSTER_ROWS = 2_000
internal const val MAX_ROSTER_BYTES = 1_000_000
private const val MAX_EMAIL_LENGTH = 254
private const val MAX_NAME_LENGTH = 100
private const val BYTE_ORDER_MARK_CODE = 0xFEFF
private val EmailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
private val FORMULA_STARTS = setOf('=', '+', '-', '@', '\t', '\r')
private val EMAIL_HEADERS = setOf("email", "e-mail", "email address", "emailaddress")
private val NAME_HEADERS = setOf("name", "display name", "display_name", "displayname", "full name", "fullname")
private val ROLE_HEADERS = setOf("role", "type")

internal const val ROSTER_TEMPLATE = "email,name,role\n" +
    "ana.cruz@school.example,Ana Cruz,student\n" +
    "ben.reyes@school.example,Ben Reyes,staff\n"

internal enum class RosterProblem {
    INVALID_EMAIL,
    DUPLICATE_EMAIL,
    UNKNOWN_ROLE,
    NAME_TOO_LONG,
}

internal data class RosterEntry(
    val line: Int,
    val email: String,
    val displayName: String,
    val role: OrgRole,
    val problem: RosterProblem?,
)

internal enum class RosterFileProblem {
    EMPTY,
    NO_EMAIL_COLUMN,
    TOO_MANY_ROWS,
    UNREADABLE,
}

internal sealed interface RosterParse {
    data class Parsed(val entries: List<RosterEntry>) : RosterParse

    data class Rejected(val problem: RosterFileProblem) : RosterParse
}

/**
 * Reads a roster file with a header row. Only the email column is required. The delimiter (comma, semicolon or tab) is
 * detected from the header, because spreadsheet programs write semicolons in some regions. A missing role means
 * student. Problems are attached to each row, so the person sees all of them before anything is created.
 */
internal fun parseRoster(text: String): RosterParse {
    val rows = readCsvRows(text.removePrefix(BYTE_ORDER_MARK_CODE.toChar().toString()))
    val header = rows.firstOrNull()?.cells.orEmpty().map { it.trim().lowercase() }
    val body = rows.drop(1)
    val columns = RosterColumns(
        email = header.indexOfFirst { it in EMAIL_HEADERS },
        name = header.indexOfFirst { it in NAME_HEADERS },
        role = header.indexOfFirst { it in ROLE_HEADERS },
    )
    val fileProblem = when {
        header.isEmpty() -> RosterFileProblem.EMPTY
        columns.email < 0 -> RosterFileProblem.NO_EMAIL_COLUMN
        body.isEmpty() -> RosterFileProblem.EMPTY
        body.size > MAX_ROSTER_ROWS -> RosterFileProblem.TOO_MANY_ROWS
        else -> null
    }
    return if (fileProblem == null) RosterParse.Parsed(entriesOf(body, columns)) else RosterParse.Rejected(fileProblem)
}

private class RosterColumns(val email: Int, val name: Int, val role: Int)

private fun entriesOf(rows: List<CsvRow>, columns: RosterColumns): List<RosterEntry> {
    val seen = mutableSetOf<String>()
    return rows.map { row ->
        val email = row.cell(columns.email).lowercase()
        val name = row.cell(columns.name).ifEmpty { email.substringBefore('@') }
        val role = roleOf(row.cell(columns.role))
        RosterEntry(
            line = row.line,
            email = email,
            displayName = name,
            role = role ?: OrgRole.STUDENT,
            problem = problemOf(email, name, role, isNewEmail = seen.add(email)),
        )
    }
}

private fun problemOf(email: String, name: String, role: OrgRole?, isNewEmail: Boolean): RosterProblem? = when {
    email.length > MAX_EMAIL_LENGTH || !EmailPattern.matches(email) -> RosterProblem.INVALID_EMAIL
    !isNewEmail -> RosterProblem.DUPLICATE_EMAIL
    role == null -> RosterProblem.UNKNOWN_ROLE
    name.length > MAX_NAME_LENGTH -> RosterProblem.NAME_TOO_LONG
    else -> null
}

/** The role named in a file, student when the cell is empty, or null when it is not a role Mesgard knows. */
private fun roleOf(cell: String): OrgRole? = when (cell.trim().lowercase()) {
    "", "student", "students", "pupil" -> OrgRole.STUDENT
    "staff", "teacher", "faculty", "employee" -> OrgRole.STAFF
    "admin", "administrator", "org_admin" -> OrgRole.ORG_ADMIN
    else -> null
}

/** One created login for the file the admin keeps: the temporary password is on this line and nowhere else. */
internal data class IssuedCredential(val email: String, val displayName: String, val temporaryPassword: String)

internal const val CREDENTIALS_HEADER = "email,name,temporary_password"

internal fun formatCredentials(credentials: List<IssuedCredential>): String = buildString {
    appendLine(CREDENTIALS_HEADER)
    credentials.forEach {
        appendLine(listOf(it.email, it.displayName, it.temporaryPassword).joinToString(",") { cell -> cell.csvCell() })
    }
}

/** Quotes a cell, and defuses a leading formula character so a spreadsheet program does not run it. */
internal fun String.csvCell(): String {
    val safe = if (firstOrNull() in FORMULA_STARTS) "'$this" else this
    val needsQuotes = safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' || it == ';' }
    return if (needsQuotes) "\"${safe.replace("\"", "\"\"")}\"" else safe
}
