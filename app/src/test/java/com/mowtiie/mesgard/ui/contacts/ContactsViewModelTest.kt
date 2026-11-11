package com.mowtiie.mesgard.ui.contacts

import app.cash.turbine.test
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.Group
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.model.StartConversationResult
import com.mowtiie.mesgard.domain.repository.ContactRepository
import com.mowtiie.mesgard.domain.repository.GroupRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ContactsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val contacts = MutableStateFlow(emptyList<Contact>())
    private val repository = mockk<ContactRepository> {
        every { observeContacts() } returns contacts
        coEvery { refresh() } returns RefreshResult.Success
    }
    private val groups = MutableStateFlow(emptyList<Group>())
    private val groupRepository = mockk<GroupRepository> {
        every { observeGroups() } returns groups
        coEvery { refresh() } returns RefreshResult.Success
    }
    private val ana = Contact("org", "ana", "Ana", OrgRole.STAFF)
    private val sectionA = Group("section-a", "org", "Section A", canStartConversation = true)

    @Test
    fun `refreshes the roster when it is created`() = runTest {
        ContactsViewModel(repository, groupRepository).uiState.test { cancelAndIgnoreRemainingEvents() }

        coVerify(exactly = 1) { repository.refresh() }
    }

    @Test
    fun `exposes the stored contacts`() = runTest {
        contacts.value = listOf(ana)

        ContactsViewModel(repository, groupRepository).uiState.test {
            val state = expectMostRecentItem()
            assertEquals(listOf("Ana"), state.contacts.map { it.displayName })
            assertFalse(state.showEmptyMessage)
        }
    }

    @Test
    fun `shows the empty message once loaded without contacts`() = runTest {
        ContactsViewModel(repository, groupRepository).uiState.test {
            assertTrue(expectMostRecentItem().showEmptyMessage)
        }
    }

    @Test
    fun `reports a refresh failure and clears it once shown`() = runTest {
        coEvery { repository.refresh() } returns RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE)
        val viewModel = ContactsViewModel(repository, groupRepository)

        viewModel.uiState.test {
            assertEquals(ContactsError.REFRESH_NETWORK_UNAVAILABLE, expectMostRecentItem().error)
            viewModel.onErrorShown()
            assertNull(awaitItem().error)
        }
    }

    @Test
    fun `starting a conversation exposes its identifier until it is opened`() = runTest {
        coEvery { repository.startDirectConversation(ana) } returns StartConversationResult.Started("conversation-1")
        val viewModel = ContactsViewModel(repository, groupRepository)

        viewModel.onContactClicked(ana)

        viewModel.uiState.test {
            assertEquals("conversation-1", expectMostRecentItem().conversationToOpen)
            viewModel.onConversationOpened()
            assertNull(awaitItem().conversationToOpen)
        }
    }

    @Test
    fun `reports a failure to start a conversation`() = runTest {
        coEvery { repository.startDirectConversation(ana) } returns
            StartConversationResult.Failure(RefreshError.UNKNOWN)
        val viewModel = ContactsViewModel(repository, groupRepository)

        viewModel.onContactClicked(ana)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(ContactsError.START_UNKNOWN, state.error)
            assertFalse(state.isStartingConversation)
        }
    }

    @Test
    fun `ignores a second tap while a conversation is being started`() = runTest {
        val pending = CompletableDeferred<StartConversationResult>()
        coEvery { repository.startDirectConversation(any()) } coAnswers { pending.await() }
        val viewModel = ContactsViewModel(repository, groupRepository)

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onContactClicked(ana)
            viewModel.onContactClicked(ana)
            assertTrue(expectMostRecentItem().isStartingConversation)
            pending.complete(StartConversationResult.Started("conversation-1"))
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { repository.startDirectConversation(any()) }
    }

    @Test
    fun `refreshes the groups together with the roster`() = runTest {
        ContactsViewModel(repository, groupRepository).uiState.test { cancelAndIgnoreRemainingEvents() }

        coVerify(exactly = 1) { groupRepository.refresh() }
    }

    @Test
    fun `reports a group refresh failure`() = runTest {
        coEvery { groupRepository.refresh() } returns RefreshResult.Failure(RefreshError.UNKNOWN)

        ContactsViewModel(repository, groupRepository).uiState.test {
            assertEquals(ContactsError.REFRESH_UNKNOWN, expectMostRecentItem().error)
        }
    }

    @Test
    fun `hides the sections when the user cannot start any group conversation`() = runTest {
        groups.value = listOf(sectionA.copy(canStartConversation = false))

        ContactsViewModel(repository, groupRepository).uiState.test {
            val state = expectMostRecentItem()
            assertFalse(state.showSections)
            assertEquals(ContactsSection.PEOPLE, state.section)
        }
    }

    @Test
    fun `shows the selected section when the user can start group conversations`() = runTest {
        groups.value = listOf(sectionA)
        val viewModel = ContactsViewModel(repository, groupRepository)

        viewModel.uiState.test {
            val initial = expectMostRecentItem()
            assertTrue(initial.showSections)
            assertEquals(ContactsSection.PEOPLE, initial.section)
            viewModel.onSectionSelected(ContactsSection.GROUPS)
            assertEquals(ContactsSection.GROUPS, awaitItem().section)
        }
    }

    @Test
    fun `starting a group conversation exposes its identifier`() = runTest {
        coEvery { groupRepository.startGroupConversation(sectionA) } returns
            StartConversationResult.Started("conversation-2")
        val viewModel = ContactsViewModel(repository, groupRepository)

        viewModel.onGroupClicked(sectionA)

        viewModel.uiState.test { assertEquals("conversation-2", expectMostRecentItem().conversationToOpen) }
    }

    @Test
    fun `reports a failure to start a group conversation`() = runTest {
        coEvery { groupRepository.startGroupConversation(sectionA) } returns
            StartConversationResult.Failure(RefreshError.NETWORK_UNAVAILABLE)
        val viewModel = ContactsViewModel(repository, groupRepository)

        viewModel.onGroupClicked(sectionA)

        viewModel.uiState.test {
            assertEquals(ContactsError.START_NETWORK_UNAVAILABLE, expectMostRecentItem().error)
        }
    }

    @Test
    fun `does not show the empty roster message on the groups section`() = runTest {
        groups.value = listOf(sectionA)
        val viewModel = ContactsViewModel(repository, groupRepository)
        viewModel.onSectionSelected(ContactsSection.GROUPS)

        viewModel.uiState.test { assertFalse(expectMostRecentItem().showEmptyMessage) }
    }
}
