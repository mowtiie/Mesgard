package com.mowtiie.mesgard.ui.profile.picture

import androidx.lifecycle.SavedStateHandle
import com.mowtiie.mesgard.domain.avatar.AvatarPhotos
import com.mowtiie.mesgard.domain.avatar.CropSquare
import com.mowtiie.mesgard.domain.avatar.PreparedPhoto
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.ProfilePicturePolicy
import com.mowtiie.mesgard.domain.model.ProfilePictureRules
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.ProfilePictureRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProfilePictureViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<ProfilePictureRepository>()
    private val photos = mockk<AvatarPhotos>(relaxed = true)
    private val account = mockk<AccountRepository> {
        every { observeAccount() } returns flowOf(Account("Ana Cruz", null, emptyList(), "u1"))
    }
    private val photo = PreparedPhoto("working.jpg", 1000, 800)
    private val crop = CropSquare(100f, 0f, 800f)

    private fun rules(canSet: Boolean = true, version: Int? = null) =
        ProfilePictureRules("org1", "u1", ProfilePicturePolicy.EVERYONE, canSet, version, null)

    private fun viewModel(rules: ProfilePictureRules = rules()): ProfilePictureViewModel {
        coEvery { repository.rules("org1") } returns AdminResult.Success(rules)
        return ProfilePictureViewModel(SavedStateHandle(mapOf("orgId" to "org1")), repository, photos, account)
    }

    @Test
    fun `the page shows what the organization allows and the name for the initials`() {
        val viewModel = viewModel(rules(canSet = false))

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertFalse(state.canChoose)
        assertEquals("Ana Cruz", state.displayName)
    }

    @Test
    fun `a picked photo is prepared for cropping`() {
        coEvery { photos.prepare("content://photo") } returns photo
        val viewModel = viewModel()

        viewModel.onPhotoPicked("content://photo")

        assertEquals(photo, viewModel.uiState.value.photo)
        assertFalse(viewModel.uiState.value.canChoose)
    }

    @Test
    fun `a photo that cannot be read is reported`() {
        coEvery { photos.prepare(any()) } returns null
        val viewModel = viewModel()

        viewModel.onPhotoPicked("content://broken")

        assertNull(viewModel.uiState.value.photo)
        assertEquals(PictureError.UNREADABLE, viewModel.uiState.value.error)
    }

    @Test
    fun `saving sends the chosen square and finishes`() {
        coEvery { photos.prepare(any()) } returns photo
        coEvery { repository.set("org1", photo, crop) } returns AdminResult.Success(Unit)
        val viewModel = viewModel()
        viewModel.onPhotoPicked("content://photo")

        viewModel.onSaveClicked(crop)

        coVerify { repository.set("org1", photo, crop) }
        assertTrue(viewModel.uiState.value.isDone)
        assertNull(viewModel.uiState.value.photo)
    }

    @Test
    fun `a refused picture stays in the editor with the reason`() {
        coEvery { photos.prepare(any()) } returns photo
        coEvery { repository.set(any(), any(), any()) } returns AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE)
        val viewModel = viewModel()
        viewModel.onPhotoPicked("content://photo")

        viewModel.onSaveClicked(crop)

        assertEquals(photo, viewModel.uiState.value.photo)
        assertEquals(PictureError.NETWORK_UNAVAILABLE, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isDone)
    }

    @Test
    fun `cancelling the crop removes the working copy`() {
        coEvery { photos.prepare(any()) } returns photo
        val viewModel = viewModel()
        viewModel.onPhotoPicked("content://photo")

        viewModel.onCropCancelled()

        verify { photos.discard(photo, null) }
        assertNull(viewModel.uiState.value.photo)
    }

    @Test
    fun `removing the picture asks first`() {
        coEvery { repository.remove("org1") } returns AdminResult.Success(Unit)
        val viewModel = viewModel(rules(version = 2))

        viewModel.onRemoveClicked()
        assertTrue(viewModel.uiState.value.showRemoveConfirmation)
        viewModel.onRemoveConfirmed()

        coVerify { repository.remove("org1") }
        assertTrue(viewModel.uiState.value.isDone)
    }
}
