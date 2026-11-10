package com.mowtiie.mesgard.data.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mowtiie.mesgard.data.local.ConversationDao
import com.mowtiie.mesgard.data.local.MessageDao
import com.mowtiie.mesgard.data.local.MessageEntity
import com.mowtiie.mesgard.data.remote.MessageRemoteSource
import com.mowtiie.mesgard.data.remote.parseEpochMillis
import com.mowtiie.mesgard.data.remote.toNewMessageDto
import com.mowtiie.mesgard.domain.model.MessageStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import java.io.IOException

private const val TAG = "SendMessageWorker"
private const val MAX_ATTEMPTS = 5

@HiltWorker
internal class SendMessageWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
    private val remoteSource: MessageRemoteSource,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val message = inputData.getString(KEY_MESSAGE_ID)?.let { messageDao.findById(it) } ?: return Result.failure()
        return if (message.status == MessageStatus.SENT) Result.success() else send(message)
    }

    private suspend fun send(message: MessageEntity): Result {
        val orgId = conversationDao.findOrgId(message.conversationId) ?: return failPermanently(message.id)
        return try {
            val stored = remoteSource.send(message.toNewMessageDto(orgId))
            messageDao.markSent(message.id, parseEpochMillis(stored.createdAt))
            Result.success()
        } catch (exception: HttpRequestException) {
            retryOrFail(message.id, exception)
        } catch (exception: IOException) {
            retryOrFail(message.id, exception)
        } catch (exception: RestException) {
            Log.w(TAG, "The server rejected message ${message.id}", exception)
            failPermanently(message.id)
        }
    }

    private suspend fun retryOrFail(messageId: String, cause: Exception): Result {
        Log.i(TAG, "Sending message $messageId failed on attempt $runAttemptCount", cause)
        return if (runAttemptCount + 1 < MAX_ATTEMPTS) Result.retry() else failPermanently(messageId)
    }

    private suspend fun failPermanently(messageId: String): Result {
        messageDao.updateStatus(messageId, MessageStatus.FAILED)
        return Result.failure()
    }

    companion object {
        const val KEY_MESSAGE_ID = "message_id"
    }
}
