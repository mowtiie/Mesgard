package com.mowtiie.mesgard.ui.export

import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.DataExportRepository
import com.mowtiie.mesgard.domain.repository.ExportFile
import com.mowtiie.mesgard.domain.repository.ExportResult
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class DataExportViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<DataExportRepository>()
    private val accountRepository = mockk<AccountRepository>()

    private fun viewModel(role: OrgRole = OrgRole.ORG_ADMIN): DataExportViewModel {
        every { accountRepository.observeAccount() } returns
            flowOf(Account("Ana", null, listOf(AccountMembership("org", "School", role))))
        return DataExportViewModel(ObserveAdminOrganization(accountRepository), repository)
    }

    @Test
    fun `an export of a person gives the file`() = runTest {
        val file = ExportFile("/cache/export.json", "export.json")
        coEvery { repository.export("org", "u1", "Student One") } returns ExportResult.Ready(file)
        val viewModel = viewModel()

        viewModel.onExportClicked("u1", "Student One")

        assertEquals(file, viewModel.uiState.value.file)
        assertFalse(viewModel.uiState.value.isExporting)
    }

    @Test
    fun `a refused export is reported`() = runTest {
        coEvery { repository.export("org", "u1", "Student One") } returns ExportResult.Failure(AdminError.NOT_ALLOWED)
        val viewModel = viewModel()

        viewModel.onExportClicked("u1", "Student One")

        assertEquals(AdminError.NOT_ALLOWED, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.file)
    }

    @Test
    fun `someone who administers nothing exports nothing`() = runTest {
        val viewModel = viewModel(role = OrgRole.STAFF)

        viewModel.onExportClicked("u1", "Student One")

        assertEquals(AdminError.NOT_ALLOWED, viewModel.uiState.value.error)
    }

    @Test
    fun `closing the dialog starts the next one clean`() = runTest {
        val file = ExportFile("/cache/export.json", "export.json")
        coEvery { repository.export("org", "u1", "Student One") } returns ExportResult.Ready(file)
        val viewModel = viewModel()
        viewModel.onExportClicked("u1", "Student One")

        viewModel.onDialogClosed()

        assertEquals(DataExportUiState(), viewModel.uiState.value)
    }
}
