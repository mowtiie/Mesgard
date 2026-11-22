package com.mowtiie.mesgard.ui.admin.groups

import androidx.lifecycle.SavedStateHandle
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
class GroupAccessViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<GroupAccessRepository>()

    private fun viewModel() = GroupAccessViewModel(SavedStateHandle(mapOf("groupId" to "g1")), repository)

    @Test
    fun `opens the group once the reason is long enough`() {
        coEvery { repository.open("g1", "Report 114 names a student") } returns AdminResult.Success(Unit)
        val viewModel = viewModel()

        viewModel.onOpenConfirmed("too short")
        coVerify(exactly = 0) { repository.open(any(), any()) }

        viewModel.onOpenConfirmed("Report 114 names a student")
        assertTrue(viewModel.uiState.value.isOpened)
        viewModel.onOpenedHandled()
        assertFalse(viewModel.uiState.value.isOpened)
    }

    @Test
    fun `keeps the error when the server refuses`() {
        coEvery { repository.open("g1", any()) } returns AdminResult.Failure(AdminError.NOT_ALLOWED)
        val viewModel = viewModel()

        viewModel.onOpenConfirmed("Report 114 names a student")

        assertEquals(AdminError.NOT_ALLOWED, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isOpened)
    }
}
