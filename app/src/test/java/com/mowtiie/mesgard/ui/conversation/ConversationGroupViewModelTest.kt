package com.mowtiie.mesgard.ui.conversation

import androidx.lifecycle.SavedStateHandle
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.repository.ConversationGroupRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversationGroupViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val conversationGroupRepository = mockk<ConversationGroupRepository>()

    private fun viewModel() = ConversationGroupViewModel(
        SavedStateHandle(mapOf("conversationId" to "c1")),
        conversationGroupRepository,
    )

    @Test
    fun `finds the group of the conversation and forgets it once it was opened`() {
        coEvery { conversationGroupRepository.groupOf("c1") } returns AdminResult.Success("g1")
        val viewModel = viewModel()

        viewModel.onGroupInfoRequested()
        assertEquals("g1", viewModel.uiState.value.groupId)

        viewModel.onGroupOpened()
        assertNull(viewModel.uiState.value.groupId)
    }

    @Test
    fun `reports a failed lookup until it was shown`() {
        coEvery { conversationGroupRepository.groupOf("c1") } returns
            AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE)
        val viewModel = viewModel()

        viewModel.onGroupInfoRequested()
        assertTrue(viewModel.uiState.value.isFailed)
        assertNull(viewModel.uiState.value.groupId)

        viewModel.onFailureShown()
        assertFalse(viewModel.uiState.value.isFailed)
    }
}
