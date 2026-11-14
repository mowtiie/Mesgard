package com.mowtiie.mesgard.ui.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.model.Attachment
import com.mowtiie.mesgard.domain.model.DownloadResult
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.repository.AttachmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class AttachmentDownloadsUiState(
    val downloadingIds: Set<String> = emptySet(),
    val failedIds: Set<String> = emptySet(),
    val attachmentToOpen: Attachment? = null,
    val error: ConversationError? = null,
)

@HiltViewModel
internal class AttachmentDownloadsViewModel @Inject constructor(private val repository: AttachmentRepository) :
    ViewModel() {
    private val mutableState = MutableStateFlow(AttachmentDownloadsUiState())

    val uiState: StateFlow<AttachmentDownloadsUiState> = mutableState

    /** Opens the attachment once its file is on the device, downloading it first when needed. */
    fun onAttachmentClicked(attachment: Attachment) {
        viewModelScope.launch {
            val localPath = download(attachment) ?: return@launch
            mutableState.update { it.copy(attachmentToOpen = attachment.copy(localPath = localPath)) }
        }
    }

    /** Images are downloaded as soon as they scroll into view. A failed one waits for the user to retry. */
    fun onImageShown(attachment: Attachment) {
        if (attachment.localPath != null || attachment.id in mutableState.value.failedIds) return
        viewModelScope.launch { download(attachment) }
    }

    fun onRetryDownload(attachment: Attachment) {
        viewModelScope.launch { download(attachment) }
    }

    fun onAttachmentOpened() = mutableState.update { it.copy(attachmentToOpen = null) }

    fun onErrorShown() = mutableState.update { it.copy(error = null) }

    private suspend fun download(attachment: Attachment): String? {
        if (attachment.id in mutableState.value.downloadingIds) return null
        mutableState.update {
            it.copy(downloadingIds = it.downloadingIds + attachment.id, failedIds = it.failedIds - attachment.id)
        }
        val result = repository.download(attachment.id)
        mutableState.update { it.copy(downloadingIds = it.downloadingIds - attachment.id) }
        return when (result) {
            is DownloadResult.Downloaded -> result.localPath
            is DownloadResult.Failure -> {
                mutableState.update {
                    it.copy(error = result.error.toDownloadError(), failedIds = it.failedIds + attachment.id)
                }
                null
            }
        }
    }
}

private fun RefreshError.toDownloadError() = when (this) {
    RefreshError.NETWORK_UNAVAILABLE -> ConversationError.DOWNLOAD_NETWORK_UNAVAILABLE
    RefreshError.UNKNOWN -> ConversationError.DOWNLOAD_UNKNOWN
}
