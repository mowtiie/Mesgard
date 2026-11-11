package com.mowtiie.mesgard.data

import app.cash.turbine.test
import com.mowtiie.mesgard.data.local.ProfileEntity
import com.mowtiie.mesgard.data.remote.ProfileRemoteSource
import com.mowtiie.mesgard.data.remote.SenderDto
import com.mowtiie.mesgard.data.remote.TypingRemoteSource
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.testing.InMemoryDatabaseRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RealtimeTypingRepositoryTest {
    @get:Rule
    val databaseRule = InMemoryDatabaseRule()

    private val incoming = MutableSharedFlow<String>(replay = 8)
    private val sentSignals = mutableListOf<Unit>()
    private val remoteSource = object : TypingRemoteSource {
        override fun session(conversationId: String, userId: String, signals: Flow<Unit>): Flow<String> = merge(
            incoming,
            flow {
                signals.collect { sentSignals += it }
            },
        )
    }
    private val profileSource = mockk<ProfileRemoteSource>()
    private val authRepository = mockk<AuthRepository> { every { currentUserId } returns "me" }
    private var nowMillis = 0L
    private val clock = object : Clock() {
        override fun getZone() = ZoneOffset.UTC

        override fun withZone(zone: java.time.ZoneId?) = this

        override fun instant(): Instant = Instant.ofEpochMilli(nowMillis)
    }
    private val repository by lazy {
        RealtimeTypingRepository(
            remoteSource,
            profileSource,
            databaseRule.database.profileDao(),
            authRepository,
            clock,
        )
    }

    private suspend fun storeProfile(id: String, name: String) =
        databaseRule.database.profileDao().upsertAll(listOf(ProfileEntity(id, name)))

    @Test
    fun `emits the name of a participant who is typing`() = runTest {
        storeProfile("ana", "Ana")

        repository.observeTypingNames("c1", MutableSharedFlow()).test {
            incoming.emit("ana")
            testScheduler.runCurrent()
            assertEquals(listOf("Ana"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ignores the signed in user`() = runTest {
        storeProfile("me", "Me")
        storeProfile("ana", "Ana")

        repository.observeTypingNames("c1", MutableSharedFlow()).test {
            incoming.emit("me")
            testScheduler.runCurrent()
            incoming.emit("ana")
            testScheduler.runCurrent()
            assertEquals(listOf("Ana"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `lists several participants and removes each after the expiry`() = runTest {
        storeProfile("ana", "Ana")
        storeProfile("ben", "Ben")

        repository.observeTypingNames("c1", MutableSharedFlow()).test {
            incoming.emit("ana")
            testScheduler.runCurrent()
            assertEquals(listOf("Ana"), awaitItem())
            advanceTimeBy(TYPING_EXPIRY_MS / 2)
            incoming.emit("ben")
            testScheduler.runCurrent()
            assertEquals(listOf("Ana", "Ben"), awaitItem())
            advanceTimeBy(TYPING_EXPIRY_MS / 2 + 1)
            assertEquals(listOf("Ben"), awaitItem())
            advanceTimeBy(TYPING_EXPIRY_MS / 2 + 1)
            assertEquals(emptyList<String>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `typing again postpones the expiry`() = runTest {
        storeProfile("ana", "Ana")

        repository.observeTypingNames("c1", MutableSharedFlow()).test {
            incoming.emit("ana")
            testScheduler.runCurrent()
            assertEquals(listOf("Ana"), awaitItem())
            advanceTimeBy(TYPING_EXPIRY_MS - 1)
            incoming.emit("ana")
            testScheduler.runCurrent()
            assertEquals(listOf("Ana"), awaitItem())
            advanceTimeBy(TYPING_EXPIRY_MS - 1)
            expectNoEvents()
            advanceTimeBy(2)
            assertEquals(emptyList<String>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `fetches the name of an unknown participant once`() = runTest {
        coEvery { profileSource.fetch("ana") } returns SenderDto("ana", "Ana")

        repository.observeTypingNames("c1", MutableSharedFlow()).test {
            incoming.emit("ana")
            testScheduler.runCurrent()
            assertEquals(listOf("Ana"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("Ana", databaseRule.database.profileDao().findDisplayName("ana"))
    }

    @Test
    fun `skips a participant whose name cannot be found`() = runTest {
        coEvery { profileSource.fetch(any()) } throws IOException("offline")
        storeProfile("ben", "Ben")

        repository.observeTypingNames("c1", MutableSharedFlow()).test {
            incoming.emit("ana")
            testScheduler.runCurrent()
            incoming.emit("ben")
            testScheduler.runCurrent()
            assertEquals(listOf("Ben"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sends at most one signal per throttle interval`() = runTest {
        val ownTyping = flow {
            emit(Unit)
            nowMillis = TYPING_THROTTLE_MS - 1
            emit(Unit)
            nowMillis = TYPING_THROTTLE_MS
            emit(Unit)
        }

        repository.observeTypingNames("c1", ownTyping).test {
            testScheduler.runCurrent()
            assertEquals(2, sentSignals.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits nothing when nobody is signed in`() = runTest {
        every { authRepository.currentUserId } returns null

        repository.observeTypingNames("c1", MutableSharedFlow()).test { awaitComplete() }
    }

    @Test
    fun `reports no typing participants when the connection fails`() = runTest {
        val failingSource = object : TypingRemoteSource {
            override fun session(conversationId: String, userId: String, signals: Flow<Unit>): Flow<String> =
                flow { throw IOException("offline") }
        }
        val failing = RealtimeTypingRepository(
            failingSource,
            profileSource,
            databaseRule.database.profileDao(),
            authRepository,
            clock,
        )

        failing.observeTypingNames("c1", MutableSharedFlow()).test {
            assertEquals(emptyList<String>(), awaitItem())
            awaitComplete()
        }
    }
}
