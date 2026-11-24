package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.AvatarRemoteSource
import com.mowtiie.mesgard.data.remote.ProfilePictureRemoteSource
import com.mowtiie.mesgard.data.remote.ProfilePictureRulesDto
import com.mowtiie.mesgard.domain.avatar.AvatarPhotos
import com.mowtiie.mesgard.domain.avatar.CropSquare
import com.mowtiie.mesgard.domain.avatar.PreparedPhoto
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.ProfilePicturePolicy
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.AuthRepository
import io.github.jan.supabase.exceptions.RestException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemoteProfilePictureRepositoryTest {
    private val remote = mockk<ProfilePictureRemoteSource>()
    private val avatars = mockk<AvatarRemoteSource>()
    private val photos = mockk<AvatarPhotos>()
    private val auth = mockk<AuthRepository> { every { currentUserId } returns "u1" }
    private val account = mockk<AccountRepository>()
    private val repository = RemoteProfilePictureRepository(remote, avatars, photos, auth, account)
    private val photo = PreparedPhoto("working.jpg", 1000, 800)
    private val crop = CropSquare(100f, 0f, 800f)

    private fun givenCurrentVersion(version: Int?) {
        coEvery { remote.rules("org1") } returns ProfilePictureRulesDto("everyone", true, version)
        coEvery { photos.crop(photo, crop) } returns "cropped.jpg"
        every { photos.discard(any(), any()) } returns Unit
        coEvery { avatars.upload(any(), any(), any()) } returns Unit
        coEvery { remote.set(any(), any()) } returns Unit
        coEvery { avatars.delete(any()) } returns Unit
        coEvery { account.refresh() } returns RefreshResult.Success
    }

    @Test
    fun `the first picture is uploaded as version 1 and then made the picture`() = runTest {
        givenCurrentVersion(null)

        val result = repository.set("org1", photo, crop)

        assertEquals(AdminResult.Success(Unit), result)
        coVerifyOrder {
            avatars.upload("org1/u1/1", File("cropped.jpg"), "image/jpeg")
            remote.set("org1", 1)
        }
        coVerify(exactly = 0) { avatars.delete(any()) }
        verify { photos.discard(photo, "cropped.jpg") }
        coVerify { account.refresh() }
    }

    @Test
    fun `a new picture takes the next version and the older file is deleted`() = runTest {
        givenCurrentVersion(3)

        repository.set("org1", photo, crop)

        coVerify { avatars.upload("org1/u1/4", any(), any()) }
        coVerify { remote.set("org1", 4) }
        coVerify { avatars.delete("org1/u1/3") }
    }

    @Test
    fun `an older file that a report keeps is left and the new picture still counts`() = runTest {
        givenCurrentVersion(3)
        coEvery { avatars.delete(any()) } throws IOException("refused")

        assertEquals(AdminResult.Success(Unit), repository.set("org1", photo, crop))
    }

    @Test
    fun `a refused picture does not stay in storage`() = runTest {
        givenCurrentVersion(null)
        coEvery { remote.set(any(), any()) } throws mockk<RestException>(relaxed = true) {
            every { statusCode } returns 403
        }

        val result = repository.set("org1", photo, crop)

        assertEquals(AdminResult.Failure(AdminError.NOT_ALLOWED), result)
        coVerify { avatars.delete("org1/u1/1") }
        coVerify(exactly = 0) { account.refresh() }
    }

    @Test
    fun `a photo that cannot be cut makes nothing`() = runTest {
        coEvery { photos.crop(photo, crop) } returns null

        assertEquals(AdminResult.Failure(AdminError.UNKNOWN), repository.set("org1", photo, crop))
        coVerify(exactly = 0) { avatars.upload(any(), any(), any()) }
    }

    @Test
    fun `removing the picture deletes its file`() = runTest {
        givenCurrentVersion(2)
        coEvery { remote.remove("org1") } returns Unit

        assertEquals(AdminResult.Success(Unit), repository.remove("org1"))

        coVerify { avatars.delete("org1/u1/2") }
        coVerify { account.refresh() }
    }

    @Test
    fun `the rules say what the organization allows`() = runTest {
        coEvery { remote.rules("org1") } returns
            ProfilePictureRulesDto("staff_and_admins", canSet = false, version = null, blockedUntil = null)

        val rules = (repository.rules("org1") as AdminResult.Success).value

        assertEquals(ProfilePicturePolicy.STAFF_AND_ADMINS, rules.policy)
        assertEquals(false, rules.canSet)
        assertEquals("u1", rules.userId)
        assertEquals(ProfilePicturePolicy.OFF, profilePicturePolicyOf("sometimes"))
    }
}
