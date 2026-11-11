package com.mowtiie.mesgard.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.mowtiie.mesgard.data.attachment.attachmentDirectory
import com.mowtiie.mesgard.data.remote.AttachmentRemoteSource
import com.mowtiie.mesgard.data.remote.toEntity
import com.mowtiie.mesgard.domain.model.DownloadResult
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.testing.InMemoryDatabaseRule
import com.mowtiie.mesgard.testing.attachmentDto
import com.mowtiie.mesgard.testing.conversationEntity
import com.mowtiie.mesgard.testing.messageEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.File
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineFirstAttachmentRepositoryTest {
    @get:Rule
    val databaseRule = InMemoryDatabaseRule()

    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val remoteSource = mockk<AttachmentRemoteSource>()
    private val repository by lazy {
        OfflineFirstAttachmentRepository(databaseRule.database.attachmentDao(), remoteSource, application)
    }

    @Before
    fun storeMessage() = runTest {
        databaseRule.database.conversationDao().upsertAll(listOf(conversationEntity("chat")))
        databaseRule.database.messageDao().upsert(messageEntity("m1", "chat", 1_000L))
    }

    @Test
    fun `downloads an attachment, stores the file and remembers its path`() = runTest {
        storeAttachment()
        coEvery { remoteSource.download("conversation/message/a1") } returns "image-bytes".toByteArray()

        val result = repository.download("a1") as DownloadResult.Downloaded

        assertEquals("image-bytes", File(result.localPath).readText())
        assertTrue(result.localPath.startsWith(application.attachmentDirectory().absolutePath))
        assertEquals(result.localPath, databaseRule.database.attachmentDao().findByMessage("m1").single().localPath)
    }

    @Test
    fun `does not download an attachment that is already stored`() = runTest {
        val file = File(application.attachmentDirectory(), "a1").apply { writeText("stored") }
        storeAttachment(localPath = file.absolutePath)

        val result = repository.download("a1")

        assertEquals(DownloadResult.Downloaded(file.absolutePath), result)
        coVerify(exactly = 0) { remoteSource.download(any()) }
    }

    @Test
    fun `downloads again when the stored file has been removed`() = runTest {
        storeAttachment(localPath = File(application.attachmentDirectory(), "gone").absolutePath)
        coEvery { remoteSource.download(any()) } returns "again".toByteArray()

        val result = repository.download("a1") as DownloadResult.Downloaded

        assertEquals("again", File(result.localPath).readText())
    }

    @Test
    fun `reports a network failure and keeps no file`() = runTest {
        storeAttachment()
        coEvery { remoteSource.download(any()) } throws IOException("offline")

        assertEquals(DownloadResult.Failure(RefreshError.NETWORK_UNAVAILABLE), repository.download("a1"))
        assertEquals(null, databaseRule.database.attachmentDao().findByMessage("m1").single().localPath)
    }

    @Test
    fun `fails for an attachment that is not stored`() = runTest {
        assertEquals(DownloadResult.Failure(RefreshError.UNKNOWN), repository.download("missing"))
    }

    private suspend fun storeAttachment(localPath: String? = null) {
        databaseRule.database.attachmentDao().upsertAll(listOf(attachmentDto("a1").toEntity("m1", localPath)))
    }
}
