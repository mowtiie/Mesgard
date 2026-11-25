package com.mowtiie.mesgard.data

import android.util.Log
import com.mowtiie.mesgard.data.remote.AvatarRemoteSource
import com.mowtiie.mesgard.data.remote.GroupPictureRemoteSource
import com.mowtiie.mesgard.data.remote.groupAvatarObjectName
import com.mowtiie.mesgard.domain.avatar.AvatarPhotos
import com.mowtiie.mesgard.domain.avatar.CropSquare
import com.mowtiie.mesgard.domain.avatar.PreparedPhoto
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupPictureRules
import com.mowtiie.mesgard.domain.repository.GroupPictureRepository
import com.mowtiie.mesgard.domain.repository.GroupRepository
import io.github.jan.supabase.exceptions.RestException
import java.io.File
import java.io.IOException
import javax.inject.Inject

private const val TAG = "GroupPictureRepository"
private const val PICTURE_MIME_TYPE = "image/jpeg"

internal class RemoteGroupPictureRepository @Inject constructor(
    private val remote: GroupPictureRemoteSource,
    private val avatars: AvatarRemoteSource,
    private val photos: AvatarPhotos,
    private val groupRepository: GroupRepository,
) : GroupPictureRepository {
    override suspend fun rules(groupId: String): AdminResult<GroupPictureRules> = attemptAdmin {
        val dto = remote.rules(groupId)
        GroupPictureRules(groupId, dto.orgId, dto.name, dto.canSet, dto.canRemove, dto.version)
    }

    override suspend fun set(groupId: String, photo: PreparedPhoto, crop: CropSquare): AdminResult<Unit> {
        val cropped = photos.crop(photo, crop)
        val result = if (cropped == null) AdminResult.Failure(AdminError.UNKNOWN) else upload(groupId, cropped)
        if (cropped != null) photos.discard(photo, cropped)
        if (result is AdminResult.Success) groupRepository.refresh()
        return result
    }

    private suspend fun upload(groupId: String, cropped: String): AdminResult<Unit> = attemptAdmin<Unit> {
        val rules = remote.rules(groupId)
        val next = (rules.version ?: 0) + 1
        val path = groupAvatarObjectName(rules.orgId, groupId, next)
        avatars.upload(path, File(cropped), PICTURE_MIME_TYPE)
        try {
            remote.set(groupId, next)
        } catch (exception: RestException) {
            deleteQuietly(path)
            throw exception
        }
        if (rules.version != null) deleteQuietly(groupAvatarObjectName(rules.orgId, groupId, rules.version))
    }

    override suspend fun remove(groupId: String): AdminResult<Unit> {
        val result = attemptAdmin<Unit> {
            val rules = remote.rules(groupId)
            remote.remove(groupId)
            if (rules.version != null) deleteQuietly(groupAvatarObjectName(rules.orgId, groupId, rules.version))
        }
        if (result is AdminResult.Success) groupRepository.refresh()
        return result
    }

    /** An older file the server refuses to delete, because a report keeps it, is simply left. */
    private suspend fun deleteQuietly(path: String) {
        try {
            avatars.delete(path)
        } catch (exception: RestException) {
            Log.i(TAG, "The older picture was left", exception)
        } catch (exception: IOException) {
            Log.i(TAG, "The older picture was left", exception)
        }
    }
}
