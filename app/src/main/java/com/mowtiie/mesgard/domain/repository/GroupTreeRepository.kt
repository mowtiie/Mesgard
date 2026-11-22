package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupKind

/**
 * Where a group sits in the tree and what kind it is. Only organization admins change these, and it needs a
 * connection.
 */
internal interface GroupTreeRepository {
    suspend fun changeKind(groupId: String, kind: GroupKind): AdminResult<Unit>

    /**
     * Moves the group inside [parentId], or to the top when it is null. The server refuses a cycle and a tree that is
     * too deep.
     */
    suspend fun moveGroup(groupId: String, parentId: String?): AdminResult<Unit>
}
