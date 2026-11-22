package com.mowtiie.mesgard.ui.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.admin.FindGroupCreationOrganization
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Whether Chats offers New group: only to a person who may create groups, which the server says. */
@HiltViewModel
internal class NewGroupEntryViewModel @Inject constructor(
    findGroupCreationOrganization: FindGroupCreationOrganization,
) : ViewModel() {
    private val mutableCanCreateGroups = MutableStateFlow(false)
    val canCreateGroups: StateFlow<Boolean> = mutableCanCreateGroups.asStateFlow()

    init {
        viewModelScope.launch { mutableCanCreateGroups.value = findGroupCreationOrganization() != null }
    }
}
