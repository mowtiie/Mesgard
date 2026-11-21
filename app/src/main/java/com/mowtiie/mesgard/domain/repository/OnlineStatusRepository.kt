package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.RefreshResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Whether others see the signed in person online. The choice is the person's own: until they make it, administrators
 * and staff are shown and students are not.
 */
internal interface OnlineStatusRepository {
    /** The person's own choice, or null until they have made one. */
    val choice: StateFlow<Boolean?>

    suspend fun refresh(): RefreshResult

    /** Shown at once and taken back when the server refuses. */
    suspend fun setChoice(show: Boolean): RefreshResult

    /** Forgets the choice of the person who signed out. */
    fun clear()
}

/** What applies while the person has not chosen: everyone but a student is shown. */
internal fun onlineStatusDefault(isStudent: Boolean): Boolean = !isStudent
