package com.mowtiie.mesgard.data.remote

import com.mowtiie.mesgard.domain.avatar.AvatarRef
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.UploadData
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import io.ktor.util.cio.readChannel
import io.ktor.util.cio.writeChannel
import java.io.File
import javax.inject.Inject

private const val AVATAR_BUCKET = "avatars"

internal fun avatarObjectName(orgId: String, userId: String, version: Int) = "$orgId/$userId/$version"

internal fun groupAvatarObjectName(orgId: String, groupId: String, version: Int) = "$orgId/groups/$groupId/$version"

/** Where the picture of [this] is kept in Storage. */
internal fun AvatarRef.objectName(): String =
    if (isGroup) groupAvatarObjectName(orgId, userId, version) else avatarObjectName(orgId, userId, version)

internal interface AvatarRemoteSource {
    suspend fun download(storagePath: String, target: File)

    suspend fun upload(storagePath: String, file: File, mimeType: String)

    /** Deletes the file of an older picture. A picture a report keeps as evidence is refused by the server. */
    suspend fun delete(storagePath: String)
}

internal class SupabaseAvatarRemoteSource @Inject constructor(private val client: SupabaseClient) :
    AvatarRemoteSource {
    override suspend fun download(storagePath: String, target: File) {
        val channel = target.writeChannel()
        try {
            client.storage.from(AVATAR_BUCKET).downloadAuthenticated(storagePath, channel)
        } finally {
            channel.flushAndClose()
        }
    }

    override suspend fun upload(storagePath: String, file: File, mimeType: String) {
        client.storage.from(AVATAR_BUCKET).upload(storagePath, UploadData(file.readChannel(), file.length())) {
            contentType = ContentType.parse(mimeType)
        }
    }

    override suspend fun delete(storagePath: String) {
        client.storage.from(AVATAR_BUCKET).delete(storagePath)
    }
}
