package com.mowtiie.mesgard.ui.conversation

import androidx.lifecycle.SavedStateHandle
import com.mowtiie.mesgard.domain.model.PinnedMessage
import com.mowtiie.mesgard.domain.model.PinsOutcome
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.MAX_PINNED_MESSAGES
import com.mowtiie.mesgard.domain.repository.PinRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PinsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val canPin = MutableStateFlow(true)
    private val repository = mockk<PinRepository> { every { observeCanPin("c1") } returns canPin }
    private val viewModel by lazy { PinsViewModel(SavedStateHandle(mapOf("conversationId" to "c1")), repository) }

    private fun pin(number: Int) = PinnedMessage("m$number", "Ana", "Pinned $number")

    @Test
    fun `refreshing shows the pinned messages, and knows who may pin`() {
        coEvery { repository.fetch("c1") } returns PinsOutcome.Loaded(listOf(pin(2), pin(1)))

        viewModel.onRefresh()

        assertEquals(listOf("m2", "m1"), viewModel.uiState.value.pins.map { it.messageId })
        assertEquals(setOf("m1", "m2"), viewModel.uiState.value.pinnedIds)
        assertTrue(viewModel.uiState.value.canPin)
        canPin.value = false
        assertFalse(viewModel.uiState.value.canPin)
    }

    @Test
    fun `a refresh that fails keeps the pins already shown`() {
        coEvery { repository.fetch("c1") } returnsMany listOf(
            PinsOutcome.Loaded(listOf(pin(1))),
            PinsOutcome.Failure(RefreshError.NETWORK_UNAVAILABLE),
        )

        viewModel.onRefresh()
        viewModel.onRefresh()

        assertEquals(listOf("m1"), viewModel.uiState.value.pins.map { it.messageId })
    }

    @Test
    fun `pinning saves it and then shows the new list`() {
        coEvery { repository.fetch("c1") } returns PinsOutcome.Loaded(listOf(pin(3)))
        coEvery { repository.pin("c1", "m3") } returns RefreshResult.Success

        viewModel.onPin("m3")

        coVerify { repository.pin("c1", "m3") }
        assertEquals(listOf("m3"), viewModel.uiState.value.pins.map { it.messageId })
    }

    @Test
    fun `a sixth pin is refused before it is sent`() {
        coEvery { repository.fetch("c1") } returns PinsOutcome.Loaded((1..MAX_PINNED_MESSAGES).map(::pin))
        viewModel.onRefresh()

        viewModel.onPin("another")

        assertEquals(ConversationError.TOO_MANY_PINS, viewModel.uiState.value.error)
        coVerify(exactly = 0) { repository.pin(any(), "another") }
    }

    @Test
    fun `unpinning removes it and a failure shows the reason`() {
        coEvery { repository.fetch("c1") } returns PinsOutcome.Loaded(emptyList())
        coEvery { repository.unpin("m1") } returns RefreshResult.Success
        coEvery { repository.unpin("m2") } returns RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE)

        viewModel.onUnpin("m1")
        assertNull(viewModel.uiState.value.error)
        viewModel.onUnpin("m2")

        assertEquals(ConversationError.NETWORK_UNAVAILABLE, viewModel.uiState.value.error)
        viewModel.onErrorShown()
        assertNull(viewModel.uiState.value.error)
    }
}
