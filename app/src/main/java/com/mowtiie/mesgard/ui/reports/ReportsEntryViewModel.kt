package com.mowtiie.mesgard.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.reports.FindReviewOrganization
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Whether the signed in person reviews reports. The server says so, so it is false until it has answered. */
@HiltViewModel
internal class ReportsEntryViewModel @Inject constructor(findReviewOrganization: FindReviewOrganization) :
    ViewModel() {
    private val mutableIsReviewer = MutableStateFlow(false)
    val isReviewer: StateFlow<Boolean> = mutableIsReviewer.asStateFlow()

    init {
        viewModelScope.launch { mutableIsReviewer.value = findReviewOrganization() != null }
    }
}
