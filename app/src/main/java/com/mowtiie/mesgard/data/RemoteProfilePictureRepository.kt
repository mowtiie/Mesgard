package com.mowtiie.mesgard.data

import android.util.Log
import com.mowtiie.mesgard.data.remote.AvatarRemoteSource
import com.mowtiie.mesgard.data.remote.ProfilePictureRemoteSource
import com.mowtiie.mesgard.data.remote.avatarObjectName
import com.mowtiie.mesgard.data.remote.parseEpochMillis
import com.mowtiie.mesgard.domain.avatar.AvatarPhotos
import com.mowtiie.mesgard.domain.avatar.CropSquare
import com.mowtiie.mesgard.domain.avatar.PreparedPhoto
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.ProfilePicturePolicy
import com.mowtiie.mesgard.domain.model.ProfilePictureRules
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.ProfilePictureRepository
import io.github.jan.supabase.exceptions.RestException
import java.io.File
import java.io.IOException
import javax.inject.Inject

private const val TAG = "ProfilePictureRepository"
private const val PICTURE_MIME_TYPE = "image/jpeg"

internal class RemoteProfilePictureRepository @Inject constructor(
    private val remote: ProfilePictureRemoteSource,
    private val avatars: AvatarRemoteSource,
    private val photos: AvatarPhotos,
    private val authRepository: AuthRepository,
    private val accountRepository: AccountRepository,
) : ProfilePictureRepository {
    override suspend fun rules(orgId: String): AdminResult<ProfilePictureRules> = attemptAdmin {
        val dto = remote.rules(orgId)
        ProfilePictureRules(
            orgId = orgId,
            userId = authRepository.currentUserId.orEmpty(),
            policy = profilePicturePolicyOf(dto.policy),
            canSet = dto.canSet,
            version = dto.version,
            blockedUntilEpochMillis = dto.blockedUntil?.let(::parseEpochMillis),
        )
    }

    override suspend fun set(orgId: String, photo: PreparedPhoto, crop: CropSquare): AdminResult<Unit> {
        val userId = authRepository.currentUserId
        val cropped = photos.crop(photo, crop)
        val result = when {
            userId == null -> AdminResult.Failure(AdminError.NOT_ALLOWED)
            cropped == null -> AdminResult.Failure(AdminError.UNKNOWN)
            else -> upload(orgId, userId, cropped)
        }
        if (cropped != null) photos.discard(photo, cropped)
        if (result is AdminResult.Success) accountRepository.refresh()
        return result
    }

    private suspend fun upload(orgId: String, userId: String, cropped: String): AdminResult<Unit> = attemptAdmin<Unit> {
        val current = remote.rules(orgId).version
        val next = (current ?: 0) + 1
        val path = avatarObjectName(orgId, userId, next)
        avatars.upload(path, File(cropped), PICTURE_MIME_TYPE)
        try {
            remote.set(orgId, next)
        } catch (exception: RestException) {
            deleteQuietly(path)
            throw exception
        }
        if (current != null) deleteQuietly(avatarObjectName(orgId, userId, current))
    }

    override suspend fun remove(orgId: String): AdminResult<Unit> {
        val userId = authRepository.currentUserId ?: return AdminResult.Failure(AdminError.NOT_ALLOWED)
        val result = attemptAdmin<Unit> {
            val current = remote.rules(orgId).version
            remote.remove(orgId)
            if (current != null) deleteQuietly(avatarObjectName(orgId, userId, current))
        }
        if (result is AdminResult.Success) accountRepository.refresh()
        return result
    }

    override suspend fun setPolicy(orgId: String, policy: ProfilePicturePolicy): AdminResult<Unit> =
        attemptAdmin { remote.setPolicy(orgId, policy.name.lowercase()) }

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

/** Anything the app does not know counts as off, so a picture is never offered on a guess. */
internal fun profilePicturePolicyOf(wireName: String): ProfilePicturePolicy =
    ProfilePicturePolicy.entries.firstOrNull { it.name.lowercase() == wireName } ?: ProfilePicturePolicy.OFF
