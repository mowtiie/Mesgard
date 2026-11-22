package com.mowtiie.mesgard.ui.conversation

import androidx.lifecycle.SavedStateHandle
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.repository.ConversationGroupRepository
import com.mowtiie.mesgard.domain.repository.GroupAccessRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
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
    private val groupAccessRepository = mockk<GroupAccessRepository>()

    private fun viewModel() = ConversationGroupViewModel(
        SavedStateHandle(mapOf("conversationId" to "c1")),
        conversationGroupRepository,
        groupAccessRepository,
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
    fun `tells the members when an administrator last opened the group`() {
        coEvery { conversationGroupRepository.groupOf("c1") } returns AdminResult.Success("g1")
        coEvery { groupAccessRepository.openings("g1") } returns AdminResult.Success(listOf(300L, 500L, 100L))
        val viewModel = viewModel()

        viewModel.onGroupConversationShown()

        assertEquals(500L, viewModel.uiState.value.lastOpenedAtEpochMillis)
    }

    @Test
    fun `says nothing for a conversation that is not a group or when nobody opened it`() {
        coEvery { conversationGroupRepository.groupOf("c1") } returns AdminResult.Failure(AdminError.NOT_ALLOWED)
        val viewModel = viewModel()

        viewModel.onGroupConversationShown()

        assertNull(viewModel.uiState.value.lastOpenedAtEpochMillis)
        coVerify(exactly = 0) { groupAccessRepository.openings(any()) }
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
