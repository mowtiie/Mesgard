package com.mowtiie.mesgard.ui.conversation

import androidx.lifecycle.SavedStateHandle
import com.mowtiie.mesgard.domain.model.MentionCandidate
import com.mowtiie.mesgard.domain.repository.MentionRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MentionViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    private val ana = MentionCandidate("u1", "Ana Cruz")
    private val repository = mockk<MentionRepository> {
        coEvery { searchStored(any(), any()) } returns emptyList()
    }
    private val viewModel by lazy { MentionViewModel(SavedStateHandle(mapOf("conversationId" to "c1")), repository) }

    @Test
    fun `typing an @ shows the people who match after a short pause`() = runTest(dispatcher) {
        coEvery { repository.search("c1", "An") } returns listOf(ana)

        viewModel.onDraftChanged("Hello @An")
        advanceTimeBy(1_000)

        assertEquals(listOf(ana), viewModel.uiState.value.suggestions)
    }

    @Test
    fun `people from the conversation show at once and the server adds the others`() = runTest(dispatcher) {
        val ben = MentionCandidate("u2", "Ana Ben")
        coEvery { repository.searchStored("c1", "An") } returns listOf(ana)
        coEvery { repository.search("c1", "An") } returns listOf(ana, ben)

        viewModel.onDraftChanged("Hello @An")
        runCurrent()
        assertEquals(listOf(ana), viewModel.uiState.value.suggestions)

        advanceTimeBy(1_000)
        assertEquals(listOf(ana, ben), viewModel.uiState.value.suggestions)
    }

    @Test
    fun `the stored people stay when the server cannot be reached`() = runTest(dispatcher) {
        coEvery { repository.searchStored("c1", "An") } returns listOf(ana)
        coEvery { repository.search("c1", "An") } returns emptyList()

        viewModel.onDraftChanged("Hello @An")
        advanceTimeBy(1_000)

        assertEquals(listOf(ana), viewModel.uiState.value.suggestions)
    }

    @Test
    fun `no search is made when no mention is being typed`() = runTest(dispatcher) {
        viewModel.onDraftChanged("Hello there")
        advanceTimeBy(1_000)

        assertEquals(emptyList<MentionCandidate>(), viewModel.uiState.value.suggestions)
        coVerify(exactly = 0) { repository.search(any(), any()) }
    }

    @Test
    fun `choosing someone puts the name in the draft and counts as a mention`() = runTest(dispatcher) {
        coEvery { repository.search("c1", "An") } returns listOf(ana)
        viewModel.onDraftChanged("Hello @An")
        advanceTimeBy(1_000)

        val draft = viewModel.onSelected(ana, "Hello @An")

        assertEquals("Hello @Ana Cruz ", draft)
        assertEquals(emptyList<MentionCandidate>(), viewModel.uiState.value.suggestions)
        assertEquals(listOf("u1"), viewModel.mentionedIds(draft))
    }

    @Test
    fun `a mention deleted from the text is not sent, and sending clears the choices`() = runTest(dispatcher) {
        viewModel.onSelected(ana, "@")

        assertEquals(emptyList<String>(), viewModel.mentionedIds("Nobody here"))
        viewModel.onSent()
        assertEquals(emptyList<String>(), viewModel.mentionedIds("@Ana Cruz "))
    }
}
