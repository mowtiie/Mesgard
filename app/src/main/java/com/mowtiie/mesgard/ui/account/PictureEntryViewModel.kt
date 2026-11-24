package com.mowtiie.mesgard.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.ProfilePictureRules
import com.mowtiie.mesgard.domain.repository.ProfilePictureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class PictureRow(val organizationName: String, val rules: ProfilePictureRules)

/**
 * The organizations where the person has a picture or may set one. A row the server cannot answer for is left out, and
 * an organization that turned pictures off for the person shows nothing.
 */
@HiltViewModel
internal class PictureEntryViewModel @Inject constructor(private val repository: ProfilePictureRepository) :
    ViewModel() {
    private val mutableRows = MutableStateFlow<List<PictureRow>>(emptyList())
    val rows: StateFlow<List<PictureRow>> = mutableRows.asStateFlow()
    private var loaded: List<AccountMembership>? = null

    fun onMembershipsChanged(memberships: List<AccountMembership>) {
        if (memberships == loaded) return
        loaded = memberships
        viewModelScope.launch { mutableRows.value = loadRows(memberships) }
    }

    private suspend fun loadRows(memberships: List<AccountMembership>): List<PictureRow> = coroutineScope {
        memberships.map { membership ->
            async {
                (repository.rules(membership.organizationId) as? AdminResult.Success)?.value
                    ?.takeIf { it.canSet || it.version != null }
                    ?.let { PictureRow(membership.organizationName, it) }
            }
        }.mapNotNull { it.await() }
    }
}
