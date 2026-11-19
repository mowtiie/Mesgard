package com.mowtiie.mesgard.data.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val BACKOFF_DELAY_SECONDS = 10L

/** Background work that needs a connection and survives a restart. */
internal interface MessageSendScheduler {
    fun schedule(messageId: String)

    /** Tells the server that the conversation was read. A newer request for the same conversation replaces it. */
    fun scheduleReadMarker(conversationId: String)
}

internal class WorkManagerMessageSendScheduler @Inject constructor(@ApplicationContext private val context: Context) :
    MessageSendScheduler {
    override fun schedule(messageId: String) {
        val request = OneTimeWorkRequestBuilder<SendMessageWorker>()
            .setInputData(workDataOf(SendMessageWorker.KEY_MESSAGE_ID to messageId))
            .setConstraints(connected())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_SECONDS, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("send-message-$messageId", ExistingWorkPolicy.KEEP, request)
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
