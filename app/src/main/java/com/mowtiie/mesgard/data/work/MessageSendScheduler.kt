package com.mowtiie.mesgard.data.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val BACKOFF_DELAY_SECONDS = 10L

/** Background work that needs a connection and survives a restart. */
internal interface MessageSendScheduler {
    /** Messages of one conversation are sent one after the other, so they arrive in the order they were written. */
    fun schedule(messageId: String, conversationId: String)

    /** Tells the server that the conversation was read. A newer request for the same conversation replaces it. */
    fun scheduleReadMarker(conversationId: String)
}

internal class WorkManagerMessageSendScheduler @Inject constructor(@ApplicationContext private val context: Context) :
    MessageSendScheduler {
    override fun schedule(messageId: String, conversationId: String) {
        val request = OneTimeWorkRequestBuilder<SendMessageWorker>()
            .setInputData(workDataOf(SendMessageWorker.KEY_MESSAGE_ID to messageId))
            .setConstraints(connected())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_SECONDS, TimeUnit.SECONDS)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context)
            .beginUniqueWork("send-messages-$conversationId", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
            .enqueue()
    }

    override fun scheduleReadMarker(conversationId: String) {
        val request = OneTimeWorkRequestBuilder<MarkReadWorker>()
            .setInputData(workDataOf(MarkReadWorker.KEY_CONVERSATION_ID to conversationId))
            .setConstraints(connected())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_SECONDS, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("mark-read-$conversationId", ExistingWorkPolicy.REPLACE, request)
    }

    private fun connected() = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
}
