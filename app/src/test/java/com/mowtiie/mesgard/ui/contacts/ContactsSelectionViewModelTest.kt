package com.mowtiie.mesgard.ui.contacts

import app.cash.turbine.test
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.model.PreferenceError
import com.mowtiie.mesgard.domain.model.PreferenceResult
import com.mowtiie.mesgard.domain.repository.ContactRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ContactsSelectionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val ana = Contact("org", "ana", "Ana", OrgRole.MEMBER)
    private val ben = Contact("org", "ben", "Ben", OrgRole.MEMBER, isFavorite = true)
    private val contacts = MutableStateFlow(listOf(ana, ben))
    private val repository = mockk<ContactRepository> {
        every { observeContacts() } returns contacts
        coEvery { setFavorite(any(), any()) } returns PreferenceResult.Success
    }
    private val viewModel by lazy { ContactsSelectionViewModel(repository) }

    @Test
    fun `one person selected can be messaged, several cannot`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onToggled(ana)
            assertEquals(ana, awaitItem().single)
            viewModel.onToggled(ben)
            assertNull(awaitItem().single)
            viewModel.onSelectionClosed()
            assertFalse(awaitItem().isActive)
        }
    }

    @Test
    fun `favoriting adds everyone unless everyone selected already is a favorite`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onSelectAll(listOf(ana, ben))
            assertFalse(awaitItem().removesFavorites)

            viewModel.onFavoriteClicked()
            awaitItem()
            coVerify { repository.setFavorite(setOf(ana, ben), true) }

            viewModel.onToggled(ben)
            assertTrue(awaitItem().removesFavorites)
            viewModel.onFavoriteClicked()
            awaitItem()
            coVerify { repository.setFavorite(setOf(ben), false) }
        }
    }

    @Test
    fun `a refused change is reported and can be dismissed`() = runTest {
        coEvery { repository.setFavorite(any(), any()) } returns
            PreferenceResult.Failure(PreferenceError.TOO_MANY_FAVORITES)

        viewModel.uiState.test {
            awaitItem()
            viewModel.onToggled(ana)
            awaitItem()
            viewModel.onFavoriteClicked()
            var state = awaitItem()
            while (state.error == null) state = awaitItem()
            assertEquals(PreferenceError.TOO_MANY_FAVORITES, state.error)

            viewModel.onErrorShown()
            assertNull(awaitItem().error)
        }
    }
}
