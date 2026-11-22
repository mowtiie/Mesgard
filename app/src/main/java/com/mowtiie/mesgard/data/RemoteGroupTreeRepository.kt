package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.GroupTreeRemoteSource
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupKind
import com.mowtiie.mesgard.domain.repository.GroupTreeRepository
import javax.inject.Inject

internal class RemoteGroupTreeRepository @Inject constructor(private val remote: GroupTreeRemoteSource) :
    GroupTreeRepository {
    override suspend fun changeKind(groupId: String, kind: GroupKind): AdminResult<Unit> =
        attemptAdmin { check(remote.setKind(groupId, kind.toWireName())) { "No group was changed" } }

    override suspend fun moveGroup(groupId: String, parentId: String?): AdminResult<Unit> =
        attemptAdmin { check(remote.setParent(groupId, parentId)) { "No group was changed" } }
}
