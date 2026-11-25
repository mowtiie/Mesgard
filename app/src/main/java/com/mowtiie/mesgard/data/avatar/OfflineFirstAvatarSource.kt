package com.mowtiie.mesgard.data.avatar

import android.content.Context
import android.util.Log
import com.mowtiie.mesgard.data.attachment.AttachmentVault
import com.mowtiie.mesgard.data.local.MesgardDatabase
import com.mowtiie.mesgard.data.remote.AvatarRemoteSource
import com.mowtiie.mesgard.data.remote.objectName
import com.mowtiie.mesgard.domain.avatar.AvatarRef
import com.mowtiie.mesgard.domain.avatar.AvatarSource
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal const val AVATAR_DIRECTORY = "avatars"
private const val TAG = "AvatarSource"
private const val PARTIAL_SUFFIX = ".part"
private const val EVIDENCE_PREFIX = "evidence_"
private const val RETRY_AFTER_MS = 60_000L

/** Profile pictures are kept next to the attachments, outside backups and encrypted by the same vault. */
@Singleton
internal class OfflineFirstAvatarSource @Inject constructor(
    private val remote: AvatarRemoteSource,
    private val vault: AttachmentVault,
    database: MesgardDatabase,
    @ApplicationContext private val context: Context,
) : AvatarSource {
    private val profileDao = database.profileDao()
    private val lock = Mutex()
    private val failedAt = mutableMapOf<String, Long>()

    override fun observeRef(userId: String): Flow<AvatarRef?> =
        profileDao.observeAvatarRef(userId).map { stored -> stored?.let { AvatarRef(it.orgId, it.userId, it.version) } }

    override suspend fun localPath(ref: AvatarRef): String? = obtain(fileOf(ref), ref.objectName()) {
        removeOlderVersions(ref, keep = it)
    }

    override suspend fun evidencePath(storagePath: String): String? =
        obtain(File(avatarDirectory(), EVIDENCE_PREFIX + storagePath.replace('/', '_')), storagePath) {}

    private suspend fun obtain(file: File, objectName: String, afterStoring: (File) -> Unit): String? =
        withContext(Dispatchers.IO) {
            if (file.isFile) return@withContext file.path
            lock.withLock {
                if (file.isFile) return@withLock file.path
                if (recentlyFailed(file.name)) return@withLock null
                fetch(objectName, file, afterStoring)
            }
        }

    private suspend fun fetch(objectName: String, file: File, afterStoring: (File) -> Unit): String? {
        val partial = File(file.parentFile, file.name + PARTIAL_SUFFIX)
        return try {
            remote.download(objectName, partial)
            check(partial.renameTo(file)) { "Cannot store the picture" }
            vault.seal(file)
            afterStoring(file)
            file.path
        } catch (exception: HttpRequestException) {
            rememberFailure(exception, partial, file)
        } catch (exception: RestException) {
            rememberFailure(exception, partial, file)
        } catch (exception: IOException) {
            rememberFailure(exception, partial, file)
        }
    }

    private fun rememberFailure(exception: Exception, partial: File, file: File): String? {
        Log.i(TAG, "The picture could not be fetched", exception)
        partial.delete()
        failedAt[file.name] = System.currentTimeMillis()
        return null
    }

    private fun recentlyFailed(name: String): Boolean =
        failedAt[name]?.let { System.currentTimeMillis() - it < RETRY_AFTER_MS } == true

    private fun removeOlderVersions(ref: AvatarRef, keep: File) {
        val prefix = "${ref.orgId}_${ref.userId}_"
        keep.parentFile?.listFiles().orEmpty()
            .filter { it.name.startsWith(prefix) && it != keep }
            .forEach { it.delete() }
    }

    private fun avatarDirectory(): File = File(context.noBackupFilesDir, AVATAR_DIRECTORY).also { it.mkdirs() }

    private fun fileOf(ref: AvatarRef): File = File(avatarDirectory(), "${ref.orgId}_${ref.userId}_${ref.version}")
}
