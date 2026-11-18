package com.mowtiie.mesgard.ui.conversation

import androidx.lifecycle.ViewModel
import com.mowtiie.mesgard.domain.model.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val QUOTE_LENGTH = 120

/** The message the next one answers, shown above the composer until it is sent or dropped. */
internal data class ReplyTarget(val messageId: String, val senderName: String?, val text: String)

@HiltViewModel
internal class ReplyViewModel @Inject constructor() : ViewModel() {
    private val mutableTarget = MutableStateFlow<ReplyTarget?>(null)
    val target: StateFlow<ReplyTarget?> = mutableTarget.asStateFlow()

    fun onReplyRequested(message: Message) {
        mutableTarget.value = ReplyTarget(message.id, message.senderName, message.body.take(QUOTE_LENGTH))
    }

    fun onReplyDropped() {
        mutableTarget.value = null
    }
}
