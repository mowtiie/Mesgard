package com.mowtiie.mesgard.domain.admin

import com.mowtiie.mesgard.domain.model.GroupKind
import com.mowtiie.mesgard.domain.model.GroupRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GroupCsvTest {
    private fun parsed(text: String) = (parseGroupFile(text) as GroupParse.Parsed).rows

    @Test
    fun `reads groups, their places and the people in them`() {
        val rows = parsed(
            """
            group,parent,kind,email,role
            Grade 11,,class,,
            STEM,Grade 11,section,Ana@School.example,owner
            STEM,Grade 11,section,ben@school.example,
            """.trimIndent(),
        )

        assertEquals(listOf("Grade 11", "STEM", "STEM"), rows.map { it.group })
        assertEquals(listOf(null, "Grade 11", "Grade 11"), rows.map { it.parent })
        assertEquals(listOf(GroupKind.CLASS, GroupKind.SECTION, GroupKind.SECTION), rows.map { it.kind })
        assertEquals(listOf(null, "ana@school.example", "ben@school.example"), rows.map { it.email })
        assertEquals(listOf(GroupRole.MEMBER, GroupRole.OWNER, GroupRole.MEMBER), rows.map { it.role })
        assertEquals(listOf(2, 3, 4), rows.map { it.line })
        assertEquals(List(3) { null }, rows.map { it.problem })
    }

    @Test
    fun `only the group column is required and the others default`() {
        val rows = parsed("section\nSTEM\n")

        assertEquals(GroupKind.OTHER, rows.single().kind)
        assertNull(rows.single().parent)
        assertNull(rows.single().email)
    }

    @Test
    fun `reads a file with semicolons and a byte order mark`() {
        val rows = parsed("group;kind\nChess;club\n")

        assertEquals(GroupKind.CLUB, rows.single().kind)
    }

    @Test
    fun `marks the problems of a row without rejecting the file`() {
        val rows = parsed(
            """
            group,kind,email,role
            ,club,,
            Chess,house,,
            Robotics,club,not-an-email,
            Debate,club,ana@school.example,boss
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                GroupRowProblem.NO_GROUP_NAME,
                GroupRowProblem.UNKNOWN_KIND,
                GroupRowProblem.INVALID_EMAIL,
                GroupRowProblem.UNKNOWN_ROLE,
            ),
            rows.map { it.problem },
        )
    }

    @Test
    fun `rejects a file without a group column, without rows or with too many`() {
        assertEquals(
            GroupParse.Rejected(GroupFileProblem.NO_GROUP_COLUMN),
            parseGroupFile("email,role\na@b.example,owner\n"),
        )
        assertEquals(GroupParse.Rejected(GroupFileProblem.EMPTY), parseGroupFile("group,kind\n"))
        assertEquals(GroupParse.Rejected(GroupFileProblem.EMPTY), parseGroupFile(""))
        val tooMany = "group\n" + (1..MAX_ROSTER_ROWS + 1).joinToString("\n") { "Group $it" }
        assertEquals(GroupParse.Rejected(GroupFileProblem.TOO_MANY_ROWS), parseGroupFile(tooMany))
    }
}
