package com.mowtiie.mesgard.ui.admin.groups

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.mowtiie.mesgard.domain.avatar.AvatarPhotos
import com.mowtiie.mesgard.domain.avatar.AvatarRef
import com.mowtiie.mesgard.domain.avatar.CropSquare
import com.mowtiie.mesgard.domain.avatar.PreparedPhoto
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.repository.GroupPictureRepository
import com.mowtiie.mesgard.ui.navigation.GroupPictureRoute
import com.mowtiie.mesgard.ui.profile.picture.PictureEditor
import com.mowtiie.mesgard.ui.profile.picture.PictureRules
import com.mowtiie.mesgard.ui.profile.picture.PictureViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** The picture of a group, set by administrators and by the owners and moderators of the group. */
@HiltViewModel
internal class GroupPictureViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: GroupPictureRepository,
    photos: AvatarPhotos,
) : PictureViewModel(photos, GroupPictureEditor(savedStateHandle.toRoute<GroupPictureRoute>().groupId, repository))

private class GroupPictureEditor(private val groupId: String, private val repository: GroupPictureRepository) :
    PictureEditor {
    override suspend fun rules(): AdminResult<PictureRules> = when (val result = repository.rules(groupId)) {
        is AdminResult.Success -> AdminResult.Success(
            PictureRules(
                ref = result.value.version?.let { AvatarRef(result.value.orgId, groupId, it, isGroup = true) },
                canSet = result.value.canSet,
                canRemove = result.value.canRemove && result.value.version != null,
                displayName = result.value.name,
            ),
        )
        is AdminResult.Failure -> result
    }

    override suspend fun save(photo: PreparedPhoto, crop: CropSquare) = repository.set(groupId, photo, crop)

    override suspend fun remove() = repository.remove(groupId)
}
