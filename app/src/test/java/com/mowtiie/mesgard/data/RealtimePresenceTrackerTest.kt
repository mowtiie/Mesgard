package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.lifecycle.InMemoryAppVisibility
import com.mowtiie.mesgard.data.remote.PresenceMembership
import com.mowtiie.mesgard.data.remote.PresenceRemoteSource
import com.mowtiie.mesgard.data.sync.RealtimePresenceTracker
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.OnlineStatusRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RealtimePresenceTrackerTest {
    private val onlineByOrganization = mutableMapOf<String, MutableStateFlow<Set<String>>>()
    private var openSessions = 0
    private var failuresBeforeFetching = 0
    private var organizationIds = listOf("org-a")
    private val studentOrganizations = mutableSetOf<String>()
    private val announcements = mutableMapOf<String, Boolean>()
    private val choice = MutableStateFlow<Boolean?>(null)
    private val onlineStatus = mockk<OnlineStatusRepository>(relaxed = true) {
        every { this@mockk.choice } returns
            this@RealtimePresenceTrackerTest.choice
    }
    private val source = object : PresenceRemoteSource {
        override suspend fun fetchMemberships(userId: String): List<PresenceMembership> {
            if (failuresBeforeFetching > 0) {
                failuresBeforeFetching--
                throw IOException("offline")
            }
            return organizationIds.map { PresenceMembership(it, isStudent = it in studentOrganizations) }
        }

        override fun session(organizationId: String, userId: String, announce: Boolean): Flow<Set<String>> {
            announcements[organizationId] = announce
            return onlineByOrganization.getOrPut(organizationId) { MutableStateFlow(emptySet()) }
                .onStart { openSessions++ }
                .onCompletion { openSessions-- }
        }
    }
    private val authRepository = mockk<AuthRepository> { every { currentUserId } returns "me" }
    private val visibility = InMemoryAppVisibility()
    private val tracker = RealtimePresenceTracker(source, authRepository, visibility, onlineStatus)

    @Test
    fun `does not track while the app is in the background`() = runTest {
        tracker.start(backgroundScope)
        runCurrent()

        assertEquals(0, openSessions)
        assertEquals(emptySet<String>(), tracker.onlineUserIds.value)
    }

    @Test
    fun `publishes the online users while the app is in the foreground`() = runTest {
        tracker.start(backgroundScope)
        visibility.setForeground(true)
        runCurrent()

        onlineByOrganization.getValue("org-a").value = setOf("ana", "me")
        runCurrent()

        assertEquals(setOf("ana", "me"), tracker.onlineUserIds.value)
    }

    @Test
    fun `merges the online users of every organization`() = runTest {
        organizationIds = listOf("org-a", "org-b")
        tracker.start(backgroundScope)
        visibility.setForeground(true)
        runCurrent()

        onlineByOrganization.getValue("org-a").value = setOf("ana")
        onlineByOrganization.getValue("org-b").value = setOf("ben")
        runCurrent()

        assertEquals(setOf("ana", "ben"), tracker.onlineUserIds.value)
    }

    @Test
    fun `leaves the channel and clears the users when the app goes to the background`() = runTest {
        tracker.start(backgroundScope)
        visibility.setForeground(true)
        runCurrent()
        onlineByOrganization.getValue("org-a").value = setOf("ana")
        runCurrent()

        visibility.setForeground(false)
        runCurrent()

        assertEquals(0, openSessions)
        assertEquals(emptySet<String>(), tracker.onlineUserIds.value)
    }

    @Test
    fun `tracks again when the app returns to the foreground`() = runTest {
        tracker.start(backgroundScope)
        visibility.setForeground(true)
        visibility.setForeground(false)
        visibility.setForeground(true)
        runCurrent()

        assertEquals(1, openSessions)
    }

    @Test
    fun `stopping leaves the channel and clears the users`() = runTest {
        tracker.start(backgroundScope)
        visibility.setForeground(true)
        runCurrent()
        onlineByOrganization.getValue("org-a").value = setOf("ana")
        runCurrent()

        tracker.stop()
        runCurrent()

        assertEquals(0, openSessions)
        assertEquals(emptySet<String>(), tracker.onlineUserIds.value)
    }

    @Test
    fun `retries after a failure to load the organizations`() = runTest {
        failuresBeforeFetching = 1
        tracker.start(backgroundScope)
        visibility.setForeground(true)
        runCurrent()
        assertEquals(0, openSessions)

        advanceTimeBy(RETRY_DELAY_MS + 1)
        runCurrent()

        assertEquals(1, openSessions)
    }

    @Test
    fun `does nothing when nobody is signed in`() = runTest {
        every { authRepository.currentUserId } returns null
        tracker.start(backgroundScope)
        visibility.setForeground(true)
        runCurrent()

        assertEquals(0, openSessions)
    }

    @Test
    fun `a student is not announced until they choose to be`() = runTest {
        organizationIds = listOf("school", "club")
        studentOrganizations += "school"
        tracker.start(backgroundScope)
        visibility.setForeground(true)
        runCurrent()

        assertEquals(false, announcements["school"])
        assertEquals(true, announcements["club"])
    }

    @Test
    fun `a choice to be shown applies to a student too`() = runTest {
        studentOrganizations += "org-a"
        choice.value = true
        tracker.start(backgroundScope)
        visibility.setForeground(true)
        runCurrent()

        assertEquals(true, announcements["org-a"])
    }

    @Test
    fun `a choice to hide applies to staff and the person still sees who is online`() = runTest {
        choice.value = false
        tracker.start(backgroundScope)
        visibility.setForeground(true)
        runCurrent()
        onlineByOrganization.getValue("org-a").value = setOf("ana")
        runCurrent()

        assertEquals(false, announcements["org-a"])
        assertEquals(setOf("ana"), tracker.onlineUserIds.value)
    }

    @Test
    fun `changing the choice announces again without leaving the screen`() = runTest {
        tracker.start(backgroundScope)
        visibility.setForeground(true)
        runCurrent()
        assertEquals(true, announcements["org-a"])

        choice.value = false
        runCurrent()

        assertEquals(false, announcements["org-a"])
        assertEquals(1, openSessions)
    }

    @Test
    fun `stopping forgets the choice of the person`() = runTest {
        tracker.start(backgroundScope)
        tracker.stop()

        verify { onlineStatus.clear() }
    }

    private companion object {
        const val RETRY_DELAY_MS = 1_000L
    }
}
