package com.mowtiie.mesgard.ui.admin.groups

import androidx.annotation.StringRes
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.GroupKind

@StringRes
internal fun GroupKind.labelResource(): Int = when (this) {
    GroupKind.SECTION -> R.string.group_kind_section
    GroupKind.CLASS -> R.string.group_kind_class
    GroupKind.DEPARTMENT -> R.string.group_kind_department
    GroupKind.CLUB -> R.string.group_kind_club
    GroupKind.OTHER -> R.string.group_kind_other
}
