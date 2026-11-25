package com.mowtiie.mesgard.ui.profile.picture

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mowtiie.mesgard.domain.avatar.AvatarPhotos
import com.mowtiie.mesgard.domain.avatar.AvatarRef
import com.mowtiie.mesgard.domain.avatar.CropSquare
import com.mowtiie.mesgard.domain.avatar.PreparedPhoto
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.ProfilePictureRepository
import com.mowtiie.mesgard.ui.navigation.ProfilePictureRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** The profile picture of the signed in person. */
@HiltViewModel
internal class ProfilePictureViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: ProfilePictureRepository,
    photos: AvatarPhotos,
    accountRepository: AccountRepository,
) : PictureViewModel(photos, ProfilePictureEditor(savedStateHandle.toRoute<ProfilePictureRoute>().orgId, repository)) {
    init {
        viewModelScope.launch { showName(accountRepository.observeAccount().first()?.displayName.orEmpty()) }
    }
}

private class ProfilePictureEditor(private val orgId: String, private val repository: ProfilePictureRepository) :
    PictureEditor {
    override suspend fun rules(): AdminResult<PictureRules> = when (val result = repository.rules(orgId)) {
        is AdminResult.Success -> AdminResult.Success(
            PictureRules(
                ref = result.value.version?.let { AvatarRef(result.value.orgId, result.value.userId, it) },
                canSet = result.value.canSet,
                canRemove = result.value.version != null,
                blockedUntilEpochMillis = result.value.blockedUntilEpochMillis,
            ),
        )
        is AdminResult.Failure -> result
    }

    override suspend fun save(photo: PreparedPhoto, crop: CropSquare) = repository.set(orgId, photo, crop)

    override suspend fun remove() = repository.remove(orgId)
}
