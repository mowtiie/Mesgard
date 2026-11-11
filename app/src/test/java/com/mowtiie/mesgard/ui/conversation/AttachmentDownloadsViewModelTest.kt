package com.mowtiie.mesgard.ui.conversation

import app.cash.turbine.test
import com.mowtiie.mesgard.domain.model.Attachment
import com.mowtiie.mesgard.domain.model.DownloadResult
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.repository.AttachmentRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AttachmentDownloadsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<AttachmentRepository>()

    @Test
    fun `downloads an attachment that is clicked and then asks to open it`() = runTest {
        coEvery { repository.download("a1") } returns DownloadResult.Downloaded("/files/a1")
        val viewModel = AttachmentDownloadsViewModel(repository)

        viewModel.onAttachmentClicked(attachment("a1"))

        viewModel.uiState.test { assertEquals("/files/a1", expectMostRecentItem().attachmentToOpen?.localPath) }
        viewModel.onAttachmentOpened()
        viewModel.uiState.test { assertEquals(null, expectMostRecentItem().attachmentToOpen) }
    }

    @Test
    fun `reports a failed download and does not open the attachment`() = runTest {
        coEvery { repository.download("a1") } returns DownloadResult.Failure(RefreshError.NETWORK_UNAVAILABLE)
        val viewModel = AttachmentDownloadsViewModel(repository)

        viewModel.onAttachmentClicked(attachment("a1"))

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(ConversationError.NETWORK_UNAVAILABLE, state.error)
            assertEquals(null, state.attachmentToOpen)
            assertTrue(state.downloadingIds.isEmpty())
        }
        viewModel.onErrorShown()
        viewModel.uiState.test { assertEquals(null, expectMostRecentItem().error) }
    }

    @Test
    fun `downloads an image when it is shown without a stored file`() = runTest {
        coEvery { repository.download("a1") } returns DownloadResult.Downloaded("/files/a1")
        val viewModel = AttachmentDownloadsViewModel(repository)

        viewModel.onImageShown(attachment("a1"))
        viewModel.onImageShown(attachment("a2", localPath = "/files/a2"))

        coVerify(exactly = 1) { repository.download("a1") }
        coVerify(exactly = 0) { repository.download("a2") }
    }

    private fun attachment(id: String, localPath: String? = null) = Attachment(
        id = id,
        messageId = "m1",
        fileName = "$id.png",
        mimeType = "image/png",
        sizeBytes = 1_024L,
        storagePath = "chat/m1/$id",
        localPath = localPath,
    )
}
