package com.mowtiie.mesgard.ui.conversation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Which messages of the conversation are selected. It is kept while the screen turns, and is empty when closed. */
@HiltViewModel
internal class MessageSelectionViewModel @Inject constructor() : ViewModel() {
    private val mutableSelected = MutableStateFlow<Set<String>>(emptySet())
    val selected: StateFlow<Set<String>> = mutableSelected.asStateFlow()

    /** A long press starts the selection, and a tap or a long press on a selected message takes it out again. */
    fun onToggled(messageId: String) = mutableSelected.update { chosen ->
        if (messageId in chosen) chosen - messageId else chosen + messageId
    }

    fun onSelectionClosed() {
        mutableSelected.value = emptySet()
    }
}
