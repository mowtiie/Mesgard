package com.mowtiie.mesgard.ui.profile.picture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.avatar.AvatarPhotos
import com.mowtiie.mesgard.domain.avatar.AvatarRef
import com.mowtiie.mesgard.domain.avatar.CropSquare
import com.mowtiie.mesgard.domain.avatar.PreparedPhoto
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class PictureError {
    NETWORK_UNAVAILABLE,
    NOT_ALLOWED,
    UNREADABLE,
    UNKNOWN,
}

/**
 * What a picture page needs to know: the picture there is now ([ref], null when none), whether one may be set or
 * removed, when a block after a report ends, and the name to show as initials.
 */
internal data class PictureRules(
    val ref: AvatarRef?,
    val canSet: Boolean,
    val canRemove: Boolean,
    val blockedUntilEpochMillis: Long? = null,
    val displayName: String? = null,
)

internal data class PictureUiState(
    val rules: PictureRules? = null,
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

/** What a picture page does for one person or one group. */
internal interface PictureEditor {
    suspend fun rules(): AdminResult<PictureRules>

    suspend fun save(photo: PreparedPhoto, crop: CropSquare): AdminResult<Unit>

    suspend fun remove(): AdminResult<Unit>
}

/** A picture of a person or of a group: pick a photo, cut a square from it, upload it or remove it. */
internal abstract class PictureViewModel(private val photos: AvatarPhotos, private val editor: PictureEditor) :
    ViewModel() {
    private val mutableUiState = MutableStateFlow(PictureUiState())
    val uiState: StateFlow<PictureUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            val result = editor.rules()
            mutableUiState.update {
                when (result) {
                    is AdminResult.Success -> it.copy(
                        rules = result.value,
                        isLoading = false,
                        displayName = result.value.displayName ?: it.displayName,
                    )
                    is AdminResult.Failure -> it.copy(isLoading = false, error = result.error.toPictureError())
                }
            }
        }
    }

    protected fun showName(name: String) = mutableUiState.update { it.copy(displayName = name) }

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
            val result = editor.save(photo, crop)
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
            val result = editor.remove()
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
}

private fun AdminError.toPictureError() = when (this) {
    AdminError.NETWORK_UNAVAILABLE -> PictureError.NETWORK_UNAVAILABLE
    AdminError.NOT_ALLOWED -> PictureError.NOT_ALLOWED
    else -> PictureError.UNKNOWN
}
