package com.mowtiie.mesgard.data.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mowtiie.mesgard.data.local.MessageDao
import com.mowtiie.mesgard.data.remote.MessageRemoteSource
import com.mowtiie.mesgard.domain.repository.AuthRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import java.io.IOException
import kotlinx.coroutines.flow.first

private const val TAG = "MarkReadWorker"
private const val MAX_ATTEMPTS = 5

/** Sends the read marker of a conversation. The newest incoming message is read when it runs, not when queued. */
@HiltWorker
internal class MarkReadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val messageDao: MessageDao,
    private val remoteSource: MessageRemoteSource,
    private val authRepository: AuthRepository,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val conversationId = inputData.getString(KEY_CONVERSATION_ID)
        val userId = authRepository.currentUserId
        return if (conversationId == null || userId == null) Result.failure() else markRead(conversationId, userId)
    }

    private suspend fun markRead(conversationId: String, userId: String): Result {
        val newestIncomingId = messageDao.observeNewestIncomingId(conversationId, userId).first()
            ?: return Result.success()
        return try {
            remoteSource.markConversationRead(conversationId, newestIncomingId)
            Result.success()
        } catch (exception: HttpRequestException) {
            retryOrFail(conversationId, exception)
        } catch (exception: IOException) {
            retryOrFail(conversationId, exception)
        } catch (exception: RestException) {
            Log.w(TAG, "The server rejected the read marker of conversation $conversationId", exception)
            Result.failure()
        }
    }

    private fun retryOrFail(conversationId: String, cause: Exception): Result {
        Log.i(TAG, "Marking conversation $conversationId as read failed on attempt $runAttemptCount", cause)
        return if (runAttemptCount + 1 < MAX_ATTEMPTS) Result.retry() else Result.failure()
    }

    companion object {
        const val KEY_CONVERSATION_ID = "conversation_id"
    }
}
