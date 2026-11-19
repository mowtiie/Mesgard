package com.mowtiie.mesgard.ui.chats

import androidx.annotation.StringRes
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.PreferenceError

@StringRes
internal fun PreferenceError.messageResource(): Int = when (this) {
    PreferenceError.NETWORK_UNAVAILABLE -> R.string.refresh_error_network_unavailable
    PreferenceError.TOO_MANY_PINNED -> R.string.preference_error_too_many_pinned
    PreferenceError.TOO_MANY_FAVORITES -> R.string.preference_error_too_many_favorites
    PreferenceError.UNKNOWN -> R.string.refresh_error_unknown
}
