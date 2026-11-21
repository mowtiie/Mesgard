package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.OnlineStatusRemoteSource
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemoteOnlineStatusRepositoryTest {
    private val remote = mockk<OnlineStatusRemoteSource>()
    private val authRepository = mockk<AuthRepository> { every { currentUserId } returns "me" }
    private val repository = RemoteOnlineStatusRepository(remote, authRepository)

    @Test
    fun `nobody has chosen until the server says so`() {
        assertNull(repository.choice.value)
    }

    @Test
    fun `refreshing reads the choice of the person`() = runTest {
        coEvery { remote.fetchChoice("me") } returns true

        assertEquals(RefreshResult.Success, repository.refresh())

        assertEquals(true, repository.choice.value)
    }

    @Test
    fun `a person who has not chosen stays without a choice`() = runTest {
        coEvery { remote.fetchChoice("me") } returns null

        repository.refresh()

        assertNull(repository.choice.value)
    }

    @Test
    fun `a choice shows at once and is saved`() = runTest {
        coEvery { remote.saveChoice(any()) } returns Unit

        repository.setChoice(false)

        assertEquals(false, repository.choice.value)
        coVerify { remote.saveChoice(false) }
    }

    @Test
    fun `a choice the server refuses is taken back`() = runTest {
        coEvery { remote.fetchChoice("me") } returns true
        repository.refresh()
        coEvery { remote.saveChoice(any()) } throws IOException("offline")

        val result = repository.setChoice(false)

        assertEquals(RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE), result)
        assertEquals(true, repository.choice.value)
    }

    @Test
    fun `signing out forgets the choice`() = runTest {
        coEvery { remote.fetchChoice("me") } returns true
        repository.refresh()

        repository.clear()

        assertNull(repository.choice.value)
    }
}
