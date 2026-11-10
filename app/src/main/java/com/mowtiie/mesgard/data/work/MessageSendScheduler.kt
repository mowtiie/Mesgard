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

internal interface MessageSendScheduler {
    fun schedule(messageId: String)
}

internal class WorkManagerMessageSendScheduler @Inject constructor(@ApplicationContext private val context: Context) :
    MessageSendScheduler {
    override fun schedule(messageId: String) {
        val request = OneTimeWorkRequestBuilder<SendMessageWorker>()
            .setInputData(workDataOf(SendMessageWorker.KEY_MESSAGE_ID to messageId))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_SECONDS, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("send-message-$messageId", ExistingWorkPolicy.KEEP, request)
    }
}
