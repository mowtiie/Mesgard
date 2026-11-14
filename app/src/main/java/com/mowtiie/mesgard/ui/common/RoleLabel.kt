package com.mowtiie.mesgard.ui.common

import androidx.annotation.StringRes
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.OrgRole

@StringRes
internal fun OrgRole.labelResource(): Int = when (this) {
    OrgRole.ORG_ADMIN -> R.string.role_org_admin
    OrgRole.STAFF -> R.string.role_staff
    OrgRole.STUDENT -> R.string.role_student
    OrgRole.OTHER -> R.string.role_member
}
