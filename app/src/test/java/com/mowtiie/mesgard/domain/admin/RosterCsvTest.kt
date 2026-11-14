package com.mowtiie.mesgard.domain.admin

import com.mowtiie.mesgard.domain.model.OrgRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RosterCsvTest {
    private fun parsed(text: String) = (parseRoster(text) as RosterParse.Parsed).entries

    @Test
    fun `reads emails, names and roles from a file with a header`() {
        val entries = parsed(
            """
            email,name,role
            ana@school.example,Ana Cruz,student
            ben@school.example,Ben Reyes,teacher
            cy@school.example,Cy Diaz,Administrator
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                Triple("ana@school.example", "Ana Cruz", OrgRole.STUDENT),
                Triple("ben@school.example", "Ben Reyes", OrgRole.STAFF),
                Triple("cy@school.example", "Cy Diaz", OrgRole.ORG_ADMIN),
            ),
            entries.map { Triple(it.email, it.displayName, it.role) },
        )
        assertTrue(entries.all { it.problem == null })
        assertEquals(listOf(2, 3, 4), entries.map { it.line })
    }

    @Test
    fun `only the email column is required and a missing role means student`() {
        val entries = parsed("Email\nAna@School.Example\n")

        assertEquals("ana@school.example", entries.single().email)
        assertEquals("ana", entries.single().displayName)
        assertEquals(OrgRole.STUDENT, entries.single().role)
    }

    @Test
    fun `recognizes the delimiter, a byte order mark and other header spellings`() {
        val semicolons = parsed("E-mail;Full Name;Type\nana@school.example;Ana Cruz;staff\n")
        val tabs = parsed("email\tname\nana@school.example\tAna Cruz\n")

        assertEquals(
            Triple("ana@school.example", "Ana Cruz", OrgRole.STAFF),
            semicolons.single().let {
                Triple(it.email, it.displayName, it.role)
            },
        )
        assertEquals("Ana Cruz", tabs.single().displayName)
    }

    @Test
    fun `handles quoted cells with commas, quotes and line breaks`() {
        val entries =
            parsed("email,name\nana@school.example,\"Cruz, Ana \"\"Annie\"\"\"\nben@school.example,\"Ben\nReyes\"\n")

        assertEquals("Cruz, Ana \"Annie\"", entries[0].displayName)
        assertEquals("Ben\nReyes", entries[1].displayName)
        assertEquals(2, entries.size)
    }

    @Test
    fun `skips blank rows and tolerates windows line endings`() {
        val entries = parsed("email,name\r\n\r\nana@school.example,Ana\r\n,\r\nben@school.example,Ben\r\n")

        assertEquals(listOf("ana@school.example", "ben@school.example"), entries.map { it.email })
    }

    @Test
    fun `marks each problem on its row`() {
        val entries = parsed(
            """
            email,name,role
            not-an-email,A,student
            ana@school.example,Ana,principal
            ben@school.example,Ben,student
            BEN@school.example,Ben Again,student
            cy@school.example,${"x".repeat(101)},student
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                RosterProblem.INVALID_EMAIL,
                RosterProblem.UNKNOWN_ROLE,
                null,
                RosterProblem.DUPLICATE_EMAIL,
                RosterProblem.NAME_TOO_LONG,
            ),
            entries.map { it.problem },
        )
    }

    @Test
    fun `rejects a file that is empty or has no email column or too many rows`() {
        assertEquals(RosterParse.Rejected(RosterFileProblem.EMPTY), parseRoster("   \n"))
        assertEquals(RosterParse.Rejected(RosterFileProblem.EMPTY), parseRoster("email,name\n"))
        assertEquals(RosterParse.Rejected(RosterFileProblem.NO_EMAIL_COLUMN), parseRoster("name,role\nAna,student\n"))
        val tooMany = "email\n" + (1..MAX_ROSTER_ROWS + 1).joinToString("\n") { "u$it@school.example" }
        assertEquals(RosterParse.Rejected(RosterFileProblem.TOO_MANY_ROWS), parseRoster(tooMany))
    }

    @Test
    fun `the template can be read back without problems`() {
        val entries = parsed(ROSTER_TEMPLATE)

        assertEquals(2, entries.size)
        assertTrue(entries.all { it.problem == null })
    }

    @Test
    fun `writes the credentials with quoting and defuses spreadsheet formulas`() {
        val text = formatCredentials(
            listOf(
                IssuedCredential("ana@school.example", "Cruz, Ana", "Abc123xyz789"),
                IssuedCredential("ben@school.example", "=SUM(A1)", "p\"w"),
            ),
        )

        assertEquals(
            "email,name,temporary_password\n" +
                "ana@school.example,\"Cruz, Ana\",Abc123xyz789\n" +
                "ben@school.example,'=SUM(A1),\"p\"\"w\"\n",
            text,
        )
    }
}
