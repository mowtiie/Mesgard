package com.mowtiie.mesgard.ui.admin.groups

import androidx.lifecycle.SavedStateHandle
import com.mowtiie.mesgard.domain.model.AccessedMessage
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.repository.GroupAccessRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GroupMessagesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<GroupAccessRepository>()

    private fun message(id: String) =
        AccessedMessage(id, "Ana", "Hi $id", 1L, "t$id", isDeleted = false, hasAttachments = false)

    private fun viewModel() = GroupMessagesViewModel(SavedStateHandle(mapOf("groupId" to "g1")), repository)

    @Test
    fun `loads the first page, offers more when it is full and appends the next page`() {
        val page = (1..50).map { message("m$it") }
        coEvery { repository.messages("g1", null, 50) } returns AdminResult.Success(page)
        coEvery { repository.messages("g1", page.last(), 50) } returns AdminResult.Success(listOf(message("m51")))
        val viewModel = viewModel()
        assertTrue(viewModel.uiState.value.hasMore)

        viewModel.loadMore()

        assertEquals(51, viewModel.uiState.value.messages.size)
        assertFalse(viewModel.uiState.value.hasMore)
    }

    @Test
    fun `shows the error when the access has ended`() {
        coEvery { repository.messages("g1", null, 50) } returns AdminResult.Failure(AdminError.NOT_ALLOWED)

        val state = viewModel().uiState.value

        assertEquals(AdminError.NOT_ALLOWED, state.error)
        assertFalse(state.isLoading)
        coVerify(exactly = 1) { repository.messages(any(), any(), any()) }
    }
}
