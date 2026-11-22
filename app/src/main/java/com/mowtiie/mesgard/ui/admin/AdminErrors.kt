package com.mowtiie.mesgard.ui.admin

import androidx.annotation.StringRes
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.AdminError

@StringRes
internal fun AdminError.messageResource(): Int = when (this) {
    AdminError.NETWORK_UNAVAILABLE -> R.string.admin_error_network_unavailable
    AdminError.NOT_ALLOWED -> R.string.admin_error_not_allowed
    AdminError.NOT_A_MEMBER -> R.string.admin_error_not_a_member
    AdminError.LAST_ADMIN -> R.string.admin_error_last_admin
    AdminError.NOT_ERASABLE -> R.string.admin_error_not_erasable
    AdminError.LEGAL_HOLD -> R.string.admin_error_legal_hold
    AdminError.GROUP_NOT_EMPTY -> R.string.admin_error_group_not_empty
    AdminError.GROUP_TREE_INVALID -> R.string.admin_error_group_tree_invalid
    AdminError.UNKNOWN -> R.string.admin_error_unknown
}
