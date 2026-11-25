package com.mowtiie.mesgard.ui.reports

import androidx.lifecycle.SavedStateHandle
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.model.ReportCategory
import com.mowtiie.mesgard.domain.model.ReportDecision
import com.mowtiie.mesgard.domain.model.ReportDetail
import com.mowtiie.mesgard.domain.model.ReportStatus
import com.mowtiie.mesgard.domain.model.ReportSubject
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.ReportReviewRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
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
class ReportDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<ReportReviewRepository>()
    private val accountRepository = mockk<AccountRepository>()

    private fun detail(status: ReportStatus = ReportStatus.OPEN, isAnonymous: Boolean = true) = ReportDetail(
        id = "r1",
        category = ReportCategory.BULLYING,
        subject = ReportSubject.MESSAGE,
        status = status,
        note = null,
        isAnonymous = isAnonymous,
        reporterName = null,
        subjectName = "Staff One",
        subjectText = null,
        isMessageRemoved = false,
        resolutionNote = null,
        createdAtEpochMillis = 0L,
        evidence = null,
        files = emptyList(),
        events = emptyList(),
    )

    private fun viewModel(role: OrgRole = OrgRole.ORG_ADMIN): ReportDetailViewModel {
        every { accountRepository.observeAccount() } returns
            flowOf(Account("Ana", null, listOf(AccountMembership("org", "School", role))))
        return ReportDetailViewModel(
            SavedStateHandle(mapOf("reportId" to "r1")),
            repository,
            ObserveAdminOrganization(accountRepository),
        )
    }

    @Test
    fun `opening a report loads the case`() = runTest {
        coEvery { repository.detail("r1") } returns AdminResult.Success(detail())

        val state = viewModel().uiState.value

        assertEquals("r1", state.detail?.id)
        assertFalse(state.isLoading)
    }

    @Test
    fun `only an administrator can ask who an anonymous reporter is`() = runTest {
        coEvery { repository.detail("r1") } returns AdminResult.Success(detail())

        assertTrue(viewModel(OrgRole.ORG_ADMIN).uiState.value.showReveal)
        assertFalse(viewModel(OrgRole.STAFF).uiState.value.showReveal)
    }

    @Test
    fun `a reporter who is named is not asked about`() = runTest {
        coEvery { repository.detail("r1") } returns AdminResult.Success(detail(isAnonymous = false))

        assertFalse(viewModel(OrgRole.ORG_ADMIN).uiState.value.showReveal)
    }

    @Test
    fun `starting a review changes the status and reloads the case`() = runTest {
        coEvery { repository.detail("r1") } returnsMany listOf(
            AdminResult.Success(detail()),
            AdminResult.Success(detail(ReportStatus.IN_REVIEW)),
        )
        coEvery { repository.decide("r1", any()) } returns AdminResult.Success(Unit)
        val viewModel = viewModel()

        viewModel.onReviewStarted()

        coVerify { repository.decide("r1", ReportDecision(ReportStatus.IN_REVIEW, "", false)) }
        assertEquals(ReportStatus.IN_REVIEW, viewModel.uiState.value.detail?.status)
    }

    @Test
    fun `closing a report sends the note and the choice to remove the message`() = runTest {
        coEvery { repository.detail("r1") } returns AdminResult.Success(detail())
        coEvery { repository.decide("r1", any()) } returns AdminResult.Success(Unit)
        val viewModel = viewModel()
        viewModel.onDialogRequested(ReportDialog.Decide(ReportStatus.RESOLVED))

        viewModel.onDecisionConfirmed("Spoke with both", followUp = true)

        coVerify { repository.decide("r1", ReportDecision(ReportStatus.RESOLVED, "Spoke with both", true)) }
        assertNull(viewModel.uiState.value.dialog)
    }

    @Test
    fun `closing a report about a username can reset the username and removes no message`() = runTest {
        coEvery { repository.detail("r1") } returns AdminResult.Success(detail().copy(subject = ReportSubject.USERNAME))
        coEvery { repository.decide("r1", any()) } returns AdminResult.Success(Unit)
        val viewModel = viewModel()
        viewModel.onDialogRequested(ReportDialog.Decide(ReportStatus.RESOLVED))

        viewModel.onDecisionConfirmed("Not appropriate", followUp = true)

        coVerify {
            repository.decide(
                "r1",
                ReportDecision(ReportStatus.RESOLVED, "Not appropriate", removeMessage = false, resetUsername = true),
            )
        }
    }

    @Test
    fun `closing a report about a picture can remove it and block new pictures`() = runTest {
        coEvery { repository.detail("r1") } returns
            AdminResult.Success(detail().copy(subject = ReportSubject.PROFILE_PICTURE))
        coEvery { repository.decide("r1", any()) } returns AdminResult.Success(Unit)
        val viewModel = viewModel()
        viewModel.onDialogRequested(ReportDialog.Decide(ReportStatus.RESOLVED))

        viewModel.onDecisionConfirmed("Not appropriate", followUp = true, blockDays = 7)

        coVerify {
            repository.decide(
                "r1",
                ReportDecision(
                    ReportStatus.RESOLVED,
                    "Not appropriate",
                    removeMessage = false,
                    resetUsername = false,
                    removePicture = true,
                    blockDays = 7,
                ),
            )
        }
    }

    @Test
    fun `closing a report about a group picture removes it without a block`() = runTest {
        coEvery { repository.detail("r1") } returns
            AdminResult.Success(detail().copy(subject = ReportSubject.GROUP_PICTURE))
        coEvery { repository.decide("r1", any()) } returns AdminResult.Success(Unit)
        val viewModel = viewModel()
        viewModel.onDialogRequested(ReportDialog.Decide(ReportStatus.RESOLVED))

        viewModel.onDecisionConfirmed("Not appropriate", followUp = true, blockDays = 7)

        coVerify {
            repository.decide(
                "r1",
                ReportDecision(ReportStatus.RESOLVED, "Not appropriate", removeMessage = false, removePicture = true),
            )
        }
    }

    @Test
    fun `closing a report about a group name can rename the group`() = runTest {
        coEvery { repository.detail("r1") } returns
            AdminResult.Success(detail().copy(subject = ReportSubject.GROUP_NAME))
        coEvery { repository.decide("r1", any()) } returns AdminResult.Success(Unit)
        val viewModel = viewModel()
        viewModel.onDialogRequested(ReportDialog.Decide(ReportStatus.RESOLVED))

        viewModel.onDecisionConfirmed("Not appropriate", followUp = true, newGroupName = " Grade 10 A ")

        coVerify {
            repository.decide(
                "r1",
                ReportDecision(
                    ReportStatus.RESOLVED,
                    "Not appropriate",
                    removeMessage = false,
                    renameGroupTo = "Grade 10 A",
                ),
            )
        }
    }

    @Test
    fun `a new group name is only sent when the follow up is applied to a group name`() = runTest {
        coEvery { repository.detail("r1") } returns
            AdminResult.Success(detail().copy(subject = ReportSubject.GROUP_NAME))
        coEvery { repository.decide("r1", any()) } returns AdminResult.Success(Unit)
        val viewModel = viewModel()
        viewModel.onDialogRequested(ReportDialog.Decide(ReportStatus.RESOLVED))

        viewModel.onDecisionConfirmed("Fine as it is", followUp = false, newGroupName = "Other")

        coVerify {
            repository.decide("r1", ReportDecision(ReportStatus.RESOLVED, "Fine as it is", removeMessage = false))
        }
    }

    @Test
    fun `a block is only sent together with removing the picture`() = runTest {
        coEvery { repository.detail("r1") } returns AdminResult.Success(detail())
        coEvery { repository.decide("r1", any()) } returns AdminResult.Success(Unit)
        val viewModel = viewModel()
        viewModel.onDialogRequested(ReportDialog.Decide(ReportStatus.RESOLVED))

        viewModel.onDecisionConfirmed("Spoke with both", followUp = true, blockDays = 30)

        coVerify { repository.decide("r1", ReportDecision(ReportStatus.RESOLVED, "Spoke with both", true)) }
    }

    @Test
    fun `a decision without a question asked sends nothing`() = runTest {
        coEvery { repository.detail("r1") } returns AdminResult.Success(detail())
        val viewModel = viewModel()

        viewModel.onDecisionConfirmed("Spoke with both", followUp = false)

        coVerify(exactly = 0) { repository.decide(any(), any()) }
    }

    @Test
    fun `a refused decision is reported and the case stays as it was`() = runTest {
        coEvery { repository.detail("r1") } returns AdminResult.Success(detail())
        coEvery { repository.decide("r1", any()) } returns AdminResult.Failure(AdminError.NOT_ALLOWED)
        val viewModel = viewModel()

        viewModel.onReviewStarted()

        assertEquals(AdminError.NOT_ALLOWED, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isWorking)
    }

    @Test
    fun `revealing a reporter shows the name`() = runTest {
        coEvery { repository.detail("r1") } returns AdminResult.Success(detail())
        coEvery { repository.revealReporter("r1", "Checking a false report") } returns
            AdminResult.Success("Student One")
        val viewModel = viewModel()

        viewModel.onRevealConfirmed("Checking a false report")

        assertEquals(ReportDialog.Revealed("Student One"), viewModel.uiState.value.dialog)
    }
}
