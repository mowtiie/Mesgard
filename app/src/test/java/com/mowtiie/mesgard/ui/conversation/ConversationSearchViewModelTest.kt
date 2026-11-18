package com.mowtiie.mesgard.ui.conversation

import androidx.lifecycle.SavedStateHandle
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.SearchCursor
import com.mowtiie.mesgard.domain.model.SearchHit
import com.mowtiie.mesgard.domain.model.SearchOutcome
import com.mowtiie.mesgard.domain.model.SearchPage
import com.mowtiie.mesgard.domain.repository.ConversationSearchRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversationSearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    private val repository = mockk<ConversationSearchRepository>()
    private val viewModel by lazy {
        ConversationSearchViewModel(SavedStateHandle(mapOf("conversationId" to "c1")), repository)
    }

    private fun hit(id: String) = SearchHit(id, "Ana", "Due on Friday", 1_000L)

    @Test
    fun `a query waits a moment, then shows what the server found`() = runTest(dispatcher) {
        coEvery { repository.search("c1", "due", null) } returns
            SearchOutcome.Loaded(SearchPage(listOf(hit("m1")), null))

        viewModel.onQueryChanged("due")
        assertTrue(viewModel.uiState.value.isLoading)
        advanceTimeBy(1_000)

        assertEquals(listOf("m1"), viewModel.uiState.value.hits.map { it.messageId })
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `a query that is too short searches nothing`() = runTest(dispatcher) {
        viewModel.onQueryChanged("d")
        advanceTimeBy(1_000)

        assertFalse(viewModel.uiState.value.isLongEnough)
        assertFalse(viewModel.uiState.value.isLoading)
        coVerify(exactly = 0) { repository.search(any(), any(), any()) }
    }

    @Test
    fun `a result page that is full can be followed by the next one`() = runTest(dispatcher) {
        val cursor = SearchCursor("2026-11-18T08:00:00+00:00", "m1")
        coEvery { repository.search("c1", "due", null) } returns
            SearchOutcome.Loaded(SearchPage(listOf(hit("m1")), cursor))
        coEvery { repository.search("c1", "due", cursor) } returns
            SearchOutcome.Loaded(SearchPage(listOf(hit("m0")), null))

        viewModel.onQueryChanged("due")
        advanceTimeBy(1_000)
        assertTrue(viewModel.uiState.value.hasMore)
        viewModel.onLoadMore()
        advanceTimeBy(1_000)

        assertEquals(listOf("m1", "m0"), viewModel.uiState.value.hits.map { it.messageId })
        assertFalse(viewModel.uiState.value.hasMore)
    }

    @Test
    fun `a search that fails says so and keeps the query`() = runTest(dispatcher) {
        coEvery { repository.search("c1", "due", null) } returns SearchOutcome.Failure(RefreshError.NETWORK_UNAVAILABLE)

        viewModel.onQueryChanged("due")
        advanceTimeBy(1_000)

        assertTrue(viewModel.uiState.value.hasFailed)
        assertEquals("due", viewModel.uiState.value.query)
    }

    @Test
    fun `choosing a hit sets the jump and closing the search keeps it until it is done`() = runTest(dispatcher) {
        viewModel.onHitSelected(hit("m1"))
        viewModel.onClosed()

        assertEquals("m1", viewModel.uiState.value.jumpTo)
        assertEquals("", viewModel.uiState.value.query)
        viewModel.onJumpHandled()
        assertNull(viewModel.uiState.value.jumpTo)
    }
}
