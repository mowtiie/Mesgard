package com.mowtiie.mesgard.ui.admin.groups

import com.mowtiie.mesgard.domain.model.AdminGroup

/** A group with how deep it sits, for a list that shows the tree by indenting. */
internal data class GroupRow(val group: AdminGroup, val depth: Int)

/**
 * Puts every group under its parent, parents first and each level by name. A group whose parent is not in the list
 * counts as a top group, so nothing disappears when a parent cannot be read.
 */
internal fun flattenGroupTree(groups: List<AdminGroup>): List<GroupRow> {
    val ids = groups.mapTo(HashSet()) { it.id }
    val childrenOf = groups.sortedWith(compareBy({ it.name.lowercase() }, { it.id })).groupBy { group ->
        group.parentId?.takeIf { it in ids }
    }
    val rows = ArrayList<GroupRow>(groups.size)
    val visited = HashSet<String>()

    fun add(parentId: String?, depth: Int) {
        childrenOf[parentId].orEmpty().forEach { group ->
            if (visited.add(group.id)) {
                rows += GroupRow(group, depth)
                add(group.id, depth + 1)
            }
        }
    }
    add(null, 0)
    return rows
}
