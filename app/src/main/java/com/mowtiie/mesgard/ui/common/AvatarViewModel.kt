package com.mowtiie.mesgard.ui.common

import androidx.lifecycle.ViewModel
import com.mowtiie.mesgard.domain.avatar.AvatarRef
import com.mowtiie.mesgard.domain.avatar.AvatarSource
import com.mowtiie.mesgard.domain.model.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Gives the composition the source of profile pictures. */
@HiltViewModel
internal class AvatarViewModel @Inject constructor(val source: AvatarSource) : ViewModel()

internal fun Contact.avatarRef(): AvatarRef? = avatarVersion?.let { AvatarRef(orgId, userId, it) }
