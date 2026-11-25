package com.mowtiie.mesgard.data.avatar

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mowtiie.mesgard.data.attachment.AttachmentVault
import com.mowtiie.mesgard.data.remote.AvatarRemoteSource
import com.mowtiie.mesgard.domain.avatar.AvatarRef
import com.mowtiie.mesgard.testing.InMemoryDatabaseRule
import com.mowtiie.mesgard.testing.TEST_ORG_ID
import com.mowtiie.mesgard.testing.contactEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineFirstAvatarSourceTest {
    @get:Rule
    val databaseRule = InMemoryDatabaseRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val remote = mockk<AvatarRemoteSource>()
    private val vault = mockk<AttachmentVault>(relaxed = true)
    private val source by lazy { OfflineFirstAvatarSource(remote, vault, databaseRule.database, context) }
    private val ref = AvatarRef(TEST_ORG_ID, "u1", 1)

    private fun storedFiles() = File(context.noBackupFilesDir, AVATAR_DIRECTORY).listFiles().orEmpty().map { it.name }

    @Test
    fun `a picture is downloaded once, sealed and kept on the phone`() = runTest {
        coEvery { remote.download(any(), any()) } coAnswers { secondArg<File>().writeText("picture") }

        val first = source.localPath(ref)
        val second = source.localPath(ref)

        assertNotNull(first)
        assertEquals(first, second)
        coVerify(exactly = 1) { remote.download("$TEST_ORG_ID/u1/1", any()) }
        verify { vault.seal(File(first!!)) }
    }

    @Test
    fun `a picture that cannot be fetched is not asked for again at once`() = runTest {
        coEvery { remote.download(any(), any()) } throws IOException("offline")

        assertNull(source.localPath(ref))
        assertNull(source.localPath(ref))

        coVerify(exactly = 1) { remote.download(any(), any()) }
        assertTrue(storedFiles().none { it.endsWith(".part") })
    }

    @Test
    fun `a newer version replaces the older file of the same person`() = runTest {
        coEvery { remote.download(any(), any()) } coAnswers { secondArg<File>().writeText("picture") }
        source.localPath(ref)
        source.localPath(AvatarRef(TEST_ORG_ID, "u2", 1))

        source.localPath(ref.copy(version = 2))

        assertEquals(listOf("${TEST_ORG_ID}_u1_2", "${TEST_ORG_ID}_u2_1").sorted(), storedFiles().sorted())
        assertFalse(storedFiles().contains("${TEST_ORG_ID}_u1_1"))
    }

    @Test
    fun `a picture a report keeps is fetched for the reviewer and kept apart from the pictures of people`() = runTest {
        coEvery { remote.download(any(), any()) } coAnswers { secondArg<File>().writeText("picture") }

        val path = source.evidencePath("$TEST_ORG_ID/u1/2")

        assertNotNull(path)
        assertTrue(File(path!!).name.startsWith("evidence_"))
        coVerify { remote.download("$TEST_ORG_ID/u1/2", any()) }
        source.localPath(ref)
        assertTrue(File(path).isFile)
    }

    @Test
    fun `the picture of a group is fetched from the folder of the group`() = runTest {
        coEvery { remote.download(any(), any()) } coAnswers { secondArg<File>().writeText("picture") }

        val path = source.localPath(AvatarRef(TEST_ORG_ID, "g1", 2, isGroup = true))

        assertNotNull(path)
        coVerify { remote.download("$TEST_ORG_ID/groups/g1/2", any()) }
    }

    @Test
    fun `the version of a person comes from the roster on the phone`() = runTest {
        databaseRule.database.contactDao().upsertAll(
            listOf(contactEntity("u1").copy(avatarVersion = 3), contactEntity("u2")),
        )

        assertEquals(AvatarRef(TEST_ORG_ID, "u1", 3), source.observeRef("u1").first())
        assertNull(source.observeRef("u2").first())
    }
}
