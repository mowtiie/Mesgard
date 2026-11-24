package com.mowtiie.mesgard.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.repository.ProfileNotice
import com.mowtiie.mesgard.domain.repository.ProfileNoticeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The notices the person has not seen, asked once when the app starts for them. */
@HiltViewModel
internal class ProfileNoticeViewModel @Inject constructor(private val repository: ProfileNoticeRepository) :
    ViewModel() {
    private val mutableNotices = MutableStateFlow<List<ProfileNotice>>(emptyList())
    val notices: StateFlow<List<ProfileNotice>> = mutableNotices.asStateFlow()

    init {
        viewModelScope.launch { mutableNotices.value = repository.unseen() }
    }

    fun onAcknowledged() {
        val shown = mutableNotices.value
        mutableNotices.value = emptyList()
        viewModelScope.launch { repository.acknowledge(shown) }
    }
}
