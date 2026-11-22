package com.mowtiie.mesgard.ui.admin.groups

import com.mowtiie.mesgard.domain.model.AdminGroup
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupTreeTest {
    private fun group(id: String, name: String, parentId: String? = null) = AdminGroup(id, name, 0, parentId = parentId)

    @Test
    fun `puts a group under its parent and orders each level by name`() {
        val rows = flattenGroupTree(
            listOf(
                group("stem", "STEM", "g11"),
                group("club", "Chess"),
                group("g11", "Grade 11"),
                group("abm", "ABM", "g11"),
            ),
        )

        assertEquals(listOf("Chess", "Grade 11", "ABM", "STEM"), rows.map { it.group.name })
        assertEquals(listOf(0, 0, 1, 1), rows.map { it.depth })
    }

    @Test
    fun `a group whose parent cannot be read is shown at the top`() {
        val rows = flattenGroupTree(listOf(group("a", "Orphan", "missing")))

        assertEquals(listOf(0), rows.map { it.depth })
    }

    @Test
    fun `a loop in the data does not hang or repeat a group`() {
        val rows = flattenGroupTree(listOf(group("a", "A", "b"), group("b", "B", "a")))

        assertEquals(emptyList<GroupRow>(), rows)
    }

    @Test
    fun `goes five levels deep`() {
        val chain = (1..5).map { group("g$it", "Level $it", if (it == 1) null else "g${it - 1}") }

        assertEquals(listOf(0, 1, 2, 3, 4), flattenGroupTree(chain.reversed()).map { it.depth })
    }
}
