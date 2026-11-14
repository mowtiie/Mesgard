package com.mowtiie.mesgard.ui.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/** What the user can do with a message that could not be sent. */
@HiltViewModel
internal class FailedMessagesViewModel @Inject constructor(private val repository: MessageRepository) : ViewModel() {
    fun onRetryClicked(messageId: String) {
        viewModelScope.launch { repository.retry(messageId) }
    }

    fun onDiscardClicked(messageId: String) {
        viewModelScope.launch { repository.discard(messageId) }
    }
}
