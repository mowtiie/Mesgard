package com.mowtiie.mesgard.ui.profile.picture

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mowtiie.mesgard.domain.avatar.AvatarPhotos
import com.mowtiie.mesgard.domain.avatar.CropSquare
import com.mowtiie.mesgard.domain.avatar.PreparedPhoto
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.ProfilePictureRules
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.ProfilePictureRepository
import com.mowtiie.mesgard.ui.navigation.ProfilePictureRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class PictureError {
    NETWORK_UNAVAILABLE,
    NOT_ALLOWED,
    UNREADABLE,
    UNKNOWN,
}

internal data class ProfilePictureUiState(
    val rules: ProfilePictureRules? = null,
    val isLoading: Boolean = true,
    /** The photo being cropped, when the person has picked one. */
    val photo: PreparedPhoto? = null,
    val isWorking: Boolean = false,
    val showRemoveConfirmation: Boolean = false,
    val isDone: Boolean = false,
    val displayName: String = "",
    val error: PictureError? = null,
) {
    val canChoose: Boolean
        get() = rules?.canSet == true && !isWorking && photo == null
}

/** The profile picture of the signed in person: pick a photo, cut a square from it, upload it or remove it. */
@HiltViewModel
internal class ProfilePictureViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProfilePictureRepository,
    private val photos: AvatarPhotos,
    private val accountRepository: AccountRepository,
) : ViewModel() {
    private val orgId = savedStateHandle.toRoute<ProfilePictureRoute>().orgId
    private val mutableUiState = MutableStateFlow(ProfilePictureUiState())
    val uiState: StateFlow<ProfilePictureUiState> = mutableUiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            val name = accountRepository.observeAccount().first()?.displayName.orEmpty()
            mutableUiState.update { it.copy(displayName = name) }
        }
    }

    fun onPhotoPicked(uri: String) {
        mutableUiState.update { it.copy(isWorking = true, error = null) }
        viewModelScope.launch {
            val photo = photos.prepare(uri)
            mutableUiState.update {
                if (photo == null) {
                    it.copy(isWorking = false, error = PictureError.UNREADABLE)
                } else {
                    it.copy(isWorking = false, photo = photo)
                }
            }
        }
    }

    fun onCropCancelled() {
        mutableUiState.value.photo?.let { photos.discard(it, croppedPath = null) }
        mutableUiState.update { it.copy(photo = null) }
    }

    fun onSaveClicked(crop: CropSquare) {
        val photo = mutableUiState.value.photo ?: return
        mutableUiState.update { it.copy(isWorking = true, error = null) }
        viewModelScope.launch {
            val result = repository.set(orgId, photo, crop)
            mutableUiState.update {
                when (result) {
                    is AdminResult.Success -> it.copy(isWorking = false, photo = null, isDone = true)
                    is AdminResult.Failure -> it.copy(isWorking = false, error = result.error.toPictureError())
                }
            }
        }
    }

    fun onRemoveClicked() = mutableUiState.update { it.copy(showRemoveConfirmation = true) }

    fun onRemoveDismissed() = mutableUiState.update { it.copy(showRemoveConfirmation = false) }

    fun onRemoveConfirmed() {
        mutableUiState.update { it.copy(showRemoveConfirmation = false, isWorking = true, error = null) }
        viewModelScope.launch {
            val result = repository.remove(orgId)
            mutableUiState.update {
                when (result) {
                    is AdminResult.Success -> it.copy(isWorking = false, isDone = true)
                    is AdminResult.Failure -> it.copy(isWorking = false, error = result.error.toPictureError())
                }
            }
        }
    }

    fun onErrorShown() = mutableUiState.update { it.copy(error = null) }

    fun onDoneShown() = mutableUiState.update { it.copy(isDone = false) }

    override fun onCleared() {
        mutableUiState.value.photo?.let { photos.discard(it, croppedPath = null) }
    }

    private fun load() {
        viewModelScope.launch {
            val result = repository.rules(orgId)
            mutableUiState.update {
                when (result) {
                    is AdminResult.Success -> it.copy(rules = result.value, isLoading = false)
                    is AdminResult.Failure -> it.copy(isLoading = false, error = result.error.toPictureError())
                }
            }
        }
    }
}

private fun AdminError.toPictureError() = when (this) {
    AdminError.NETWORK_UNAVAILABLE -> PictureError.NETWORK_UNAVAILABLE
    AdminError.NOT_ALLOWED -> PictureError.NOT_ALLOWED
    else -> PictureError.UNKNOWN
}
