package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.AvatarRemoteSource
import com.mowtiie.mesgard.data.remote.GroupPictureRemoteSource
import com.mowtiie.mesgard.data.remote.GroupPictureRulesDto
import com.mowtiie.mesgard.domain.avatar.AvatarPhotos
import com.mowtiie.mesgard.domain.avatar.CropSquare
import com.mowtiie.mesgard.domain.avatar.PreparedPhoto
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupPictureRules
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.GroupRepository
import io.github.jan.supabase.exceptions.RestException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemoteGroupPictureRepositoryTest {
    private val remote = mockk<GroupPictureRemoteSource>()
    private val avatars = mockk<AvatarRemoteSource>()
    private val photos = mockk<AvatarPhotos>()
    private val groups = mockk<GroupRepository>()
    private val repository = RemoteGroupPictureRepository(remote, avatars, photos, groups)
    private val photo = PreparedPhoto("working.jpg", 1000, 800)
    private val crop = CropSquare(100f, 0f, 800f)

    private fun givenCurrentVersion(version: Int?) {
        coEvery { remote.rules("g1") } returns GroupPictureRulesDto("org1", "Grade 10", true, true, version)
        coEvery { photos.crop(photo, crop) } returns "cropped.jpg"
        every { photos.discard(any(), any()) } returns Unit
        coEvery { avatars.upload(any(), any(), any()) } returns Unit
        coEvery { remote.set(any(), any()) } returns Unit
        coEvery { avatars.delete(any()) } returns Unit
        coEvery { groups.refresh() } returns RefreshResult.Success
    }

    @Test
    fun `the first picture is uploaded into the folder of the group and then made the picture`() = runTest {
        givenCurrentVersion(null)

        val result = repository.set("g1", photo, crop)

        assertEquals(AdminResult.Success(Unit), result)
        coVerifyOrder {
            avatars.upload("org1/groups/g1/1", File("cropped.jpg"), "image/jpeg")
            remote.set("g1", 1)
        }
        coVerify(exactly = 0) { avatars.delete(any()) }
        coVerify { groups.refresh() }
    }

    @Test
    fun `a new picture takes the next version and the older file is deleted`() = runTest {
        givenCurrentVersion(3)

        repository.set("g1", photo, crop)

        coVerify { avatars.upload("org1/groups/g1/4", any(), any()) }
        coVerify { remote.set("g1", 4) }
        coVerify { avatars.delete("org1/groups/g1/3") }
    }

    @Test
    fun `an older file that a report keeps is left and the new picture still counts`() = runTest {
        givenCurrentVersion(3)
        coEvery { avatars.delete(any()) } throws IOException("refused")

        assertEquals(AdminResult.Success(Unit), repository.set("g1", photo, crop))
    }

    @Test
    fun `a refused picture does not stay in storage`() = runTest {
        givenCurrentVersion(null)
        coEvery { remote.set(any(), any()) } throws mockk<RestException>(relaxed = true) {
            every { statusCode } returns 403
        }

        val result = repository.set("g1", photo, crop)

        assertEquals(AdminResult.Failure(AdminError.NOT_ALLOWED), result)
        coVerify { avatars.delete("org1/groups/g1/1") }
        coVerify(exactly = 0) { groups.refresh() }
    }

    @Test
    fun `a photo that cannot be cut makes nothing`() = runTest {
        coEvery { photos.crop(photo, crop) } returns null

        assertEquals(AdminResult.Failure(AdminError.UNKNOWN), repository.set("g1", photo, crop))
        coVerify(exactly = 0) { avatars.upload(any(), any(), any()) }
    }

    @Test
    fun `removing the picture deletes its file and loads the groups again`() = runTest {
        givenCurrentVersion(2)
        coEvery { remote.remove("g1") } returns Unit

        assertEquals(AdminResult.Success(Unit), repository.remove("g1"))

        coVerify { avatars.delete("org1/groups/g1/2") }
        coVerify { groups.refresh() }
    }

    @Test
    fun `the rules say what the person may do with the picture`() = runTest {
        givenCurrentVersion(2)

        val rules = (repository.rules("g1") as AdminResult.Success).value

        assertEquals(GroupPictureRules("g1", "org1", "Grade 10", canSet = true, canRemove = true, version = 2), rules)
    }
}
