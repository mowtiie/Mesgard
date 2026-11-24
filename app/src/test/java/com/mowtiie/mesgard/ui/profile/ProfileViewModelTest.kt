package com.mowtiie.mesgard.ui.profile

import androidx.lifecycle.SavedStateHandle
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.model.PersonReport
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.ReportCategory
import com.mowtiie.mesgard.domain.model.ReportResult
import com.mowtiie.mesgard.domain.model.ReportSubject
import com.mowtiie.mesgard.domain.model.StartConversationResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.ContactRepository
import com.mowtiie.mesgard.domain.repository.ReportRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import com.mowtiie.mesgard.ui.reports.ReportStage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
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
class ProfileViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val person = Contact("org1", "u2", "Student Two", OrgRole.MEMBER)
    private val contacts = mockk<ContactRepository>()
    private val reports = mockk<ReportRepository>()
    private val auth = mockk<AuthRepository>()

    private fun viewModel(
        roster: List<Contact> = listOf(person),
        orgId: String? = "org1",
        me: String = "u1",
    ): ProfileViewModel {
        every { contacts.observeContacts() } returns flowOf(roster)
        coEvery { contacts.refreshIfStale() } returns mockk(relaxed = true)
        every { auth.currentUserId } returns me
        val handle = SavedStateHandle(mapOf("userId" to "u2", "orgId" to orgId))
        return ProfileViewModel(handle, contacts, reports, auth)
    }

    @Test
    fun `the profile shows the person of the roster`() = runTest {
        val state = viewModel().profile.first { !it.isLoading && it.person != null }

        assertEquals(person, state.person)
        assertFalse(state.isOwnProfile)
    }

    @Test
    fun `a person who is not in the roster is not found`() = runTest {
        val viewModel = viewModel(roster = emptyList())

        val state = viewModel.profile.first { !it.isLoading }

        assertNull(state.person)
    }

    @Test
    fun `the organization is optional, as in a conversation`() = runTest {
        val state = viewModel(orgId = null).profile.first { it.person != null }

        assertEquals("org1", state.person?.orgId)
    }

    @Test
    fun `the own profile is marked, so it has no report or message action`() = runTest {
        val state = viewModel(me = "u2").profile.first { it.person != null }

        assertTrue(state.isOwnProfile)
    }

    @Test
    fun `a report about a person asks what is reported and sends the name subject`() = runTest {
        val sent = slot<PersonReport>()
        coEvery { reports.reportPerson(capture(sent)) } returns ReportResult.Sent
        val viewModel = viewModel()
        viewModel.profile.first { it.person != null }

        viewModel.onReportRequested()
        assertTrue(viewModel.uiState.value.asksForSubject)
        assertFalse(viewModel.uiState.value.canSend)
        viewModel.onSubjectChanged(ReportSubject.NAME)
        assertEquals(
            listOf(ReportCategory.IMPERSONATION, ReportCategory.OFFENSIVE_CONTENT, ReportCategory.OTHER),
            viewModel.uiState.value.categories,
        )
        viewModel.onCategoryChanged(ReportCategory.IMPERSONATION)
        viewModel.onSendClicked()

        assertEquals(
            PersonReport(
                reportId = viewModel.uiState.value.reportId,
                orgId = "org1",
                userId = "u2",
                subject = ReportSubject.NAME,
                category = ReportCategory.IMPERSONATION,
                note = "",
                isAnonymous = false,
            ),
            sent.captured,
        )
        assertEquals(ReportStage.SENT, viewModel.uiState.value.stage)
    }

    @Test
    fun `changing what is reported clears the category, which belongs to the subject`() = runTest {
        val viewModel = viewModel()
        viewModel.onReportRequested()
        viewModel.onSubjectChanged(ReportSubject.PERSON)
        viewModel.onCategoryChanged(ReportCategory.BULLYING)

        viewModel.onSubjectChanged(ReportSubject.NAME)

        assertNull(viewModel.uiState.value.category)
    }

    @Test
    fun `message starts the conversation and then opens it`() = runTest {
        coEvery { contacts.startDirectConversation(person) } returns StartConversationResult.Started("c1")
        val viewModel = viewModel()
        viewModel.profile.first { it.person != null }

        viewModel.onMessageClicked()
        advanceUntilIdle()

        assertEquals("c1", viewModel.profile.first { it.conversationToOpen != null }.conversationToOpen)
        viewModel.onConversationOpened()
        assertNull(viewModel.profile.first { it.conversationToOpen == null }.conversationToOpen)
    }

    @Test
    fun `a conversation that cannot be started is reported once`() = runTest {
        coEvery { contacts.startDirectConversation(person) } returns
            StartConversationResult.Failure(RefreshError.NETWORK_UNAVAILABLE)
        val viewModel = viewModel()
        viewModel.profile.first { it.person != null }

        viewModel.onMessageClicked()
        advanceUntilIdle()

        assertTrue(viewModel.profile.first { it.startFailed }.startFailed)
        viewModel.onStartFailureShown()
        assertFalse(viewModel.profile.first { !it.startFailed }.startFailed)
        coVerify(exactly = 1) { contacts.startDirectConversation(person) }
    }
}
