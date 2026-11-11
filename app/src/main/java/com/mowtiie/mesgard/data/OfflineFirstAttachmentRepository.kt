package com.mowtiie.mesgard.data

import android.content.Context
import com.mowtiie.mesgard.data.attachment.attachmentDirectory
import com.mowtiie.mesgard.data.local.AttachmentDao
import com.mowtiie.mesgard.data.remote.AttachmentRemoteSource
import com.mowtiie.mesgard.data.remote.RemoteOutcome
import com.mowtiie.mesgard.data.remote.attemptRemote
import com.mowtiie.mesgard.domain.model.DownloadResult
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.repository.AttachmentRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "AttachmentRepository"
private const val PARTIAL_FILE_SUFFIX = ".part"

internal class OfflineFirstAttachmentRepository @Inject constructor(
    private val attachmentDao: AttachmentDao,
    private val remoteSource: AttachmentRemoteSource,
    @ApplicationContext private val context: Context,
) : AttachmentRepository {
    override suspend fun download(attachmentId: String): DownloadResult {
        val attachment = attachmentDao.findByIds(listOf(attachmentId)).singleOrNull()
            ?: return DownloadResult.Failure(RefreshError.UNKNOWN)
        val stored = attachment.localPath?.takeIf { File(it).isFile }
        if (stored != null) return DownloadResult.Downloaded(stored)
        val outcome = attemptRemote(TAG) {
            val bytes = remoteSource.download(attachment.storagePath)
            withContext(Dispatchers.IO) { store(attachmentId, bytes) }
        }
        return when (outcome) {
            is RemoteOutcome.Success -> {
                attachmentDao.updateLocalPath(attachmentId, outcome.value)
                DownloadResult.Downloaded(outcome.value)
            }
            is RemoteOutcome.Failure -> DownloadResult.Failure(outcome.error)
        }
    }

    private fun store(attachmentId: String, bytes: ByteArray): String {
        val target = File(context.attachmentDirectory(), attachmentId)
        val partial = File(target.parentFile, attachmentId + PARTIAL_FILE_SUFFIX)
        partial.writeBytes(bytes)
        check(partial.renameTo(target)) { "Cannot store the file of attachment $attachmentId" }
        return target.absolutePath
    }
}
