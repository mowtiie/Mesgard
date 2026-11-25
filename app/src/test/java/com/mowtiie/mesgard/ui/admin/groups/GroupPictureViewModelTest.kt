package com.mowtiie.mesgard.ui.admin.groups

import androidx.lifecycle.SavedStateHandle
import com.mowtiie.mesgard.domain.avatar.AvatarPhotos
import com.mowtiie.mesgard.domain.avatar.AvatarRef
import com.mowtiie.mesgard.domain.avatar.CropSquare
import com.mowtiie.mesgard.domain.avatar.PreparedPhoto
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupPictureRules
import com.mowtiie.mesgard.domain.repository.GroupPictureRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GroupPictureViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<GroupPictureRepository>()
    private val photos = mockk<AvatarPhotos>(relaxed = true)
    private val photo = PreparedPhoto("working.jpg", 1000, 800)
    private val crop = CropSquare(100f, 0f, 800f)

    private fun viewModel(
        canSet: Boolean = true,
        canRemove: Boolean = true,
        version: Int? = null,
    ): GroupPictureViewModel {
        coEvery { repository.rules("g1") } returns
            AdminResult.Success(GroupPictureRules("g1", "org1", "Grade 10", canSet, canRemove, version))
        return GroupPictureViewModel(SavedStateHandle(mapOf("groupId" to "g1")), repository, photos)
    }

    @Test
    fun `the page shows the picture of the group and uses its name for the initials`() {
        val state = viewModel(version = 2).uiState.value

        assertEquals(AvatarRef("org1", "g1", 2, isGroup = true), state.rules?.ref)
        assertEquals("Grade 10", state.displayName)
        assertTrue(state.canChoose)
        assertFalse(state.isLoading)
    }

    @Test
    fun `a person who cannot set the picture cannot choose a photo`() {
        assertFalse(viewModel(canSet = false).uiState.value.canChoose)
    }

    @Test
    fun `a group without a picture has nothing to remove`() {
        assertFalse(viewModel(canRemove = true, version = null).uiState.value.rules!!.canRemove)
    }

    @Test
    fun `saving the photo sends the crop for this group and finishes`() {
        coEvery { photos.prepare("content://photo") } returns photo
        coEvery { repository.set("g1", photo, crop) } returns AdminResult.Success(Unit)
        val viewModel = viewModel()

        viewModel.onPhotoPicked("content://photo")
        viewModel.onSaveClicked(crop)

        coVerify { repository.set("g1", photo, crop) }
        assertTrue(viewModel.uiState.value.isDone)
        assertNull(viewModel.uiState.value.photo)
    }

    @Test
    fun `removing the picture asks first and then removes it for this group`() {
        coEvery { repository.remove("g1") } returns AdminResult.Success(Unit)
        val viewModel = viewModel(version = 2)

        viewModel.onRemoveClicked()
        assertTrue(viewModel.uiState.value.showRemoveConfirmation)
        viewModel.onRemoveConfirmed()

        coVerify { repository.remove("g1") }
        assertTrue(viewModel.uiState.value.isDone)
    }

    @Test
    fun `a refused change is reported and the page stays`() {
        coEvery { photos.prepare(any()) } returns photo
        coEvery { repository.set(any(), any(), any()) } returns AdminResult.Failure(AdminError.NOT_ALLOWED)
        val viewModel = viewModel()

        viewModel.onPhotoPicked("content://photo")
        viewModel.onSaveClicked(crop)

        assertFalse(viewModel.uiState.value.isDone)
        assertEquals(com.mowtiie.mesgard.ui.profile.picture.PictureError.NOT_ALLOWED, viewModel.uiState.value.error)
    }
}
