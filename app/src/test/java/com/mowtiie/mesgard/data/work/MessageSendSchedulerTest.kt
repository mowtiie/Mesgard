package com.mowtiie.mesgard.data.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MessageSendSchedulerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var scheduler: WorkManagerMessageSendScheduler

    @Before
    fun initializeWorkManager() {
        val configuration = Configuration.Builder().setExecutor(SynchronousExecutor()).build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration)
        scheduler = WorkManagerMessageSendScheduler(context)
    }

    @Test
    fun `messages of one conversation wait for the one before them`() {
        scheduler.schedule("first", "chat")
        scheduler.schedule("second", "chat")

        val states = statesOf("send-messages-chat")

        assertEquals(listOf(WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED), states.sortedBy { it.ordinal })
    }

    @Test
    fun `messages of different conversations do not wait for each other`() {
        scheduler.schedule("first", "chat")
        scheduler.schedule("second", "other")

        assertEquals(listOf(WorkInfo.State.ENQUEUED), statesOf("send-messages-chat"))
        assertEquals(listOf(WorkInfo.State.ENQUEUED), statesOf("send-messages-other"))
    }

    @Test
    fun `a read marker replaces the one queued for the same conversation`() {
        scheduler.scheduleReadMarker("chat")
        scheduler.scheduleReadMarker("chat")

        assertEquals(listOf(WorkInfo.State.ENQUEUED), statesOf("mark-read-chat"))
    }

    private fun statesOf(uniqueName: String) = WorkManager.getInstance(context)
        .getWorkInfosForUniqueWork(uniqueName)
        .get()
        .map { it.state }
}
