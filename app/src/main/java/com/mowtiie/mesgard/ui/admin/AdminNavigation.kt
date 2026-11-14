package com.mowtiie.mesgard.ui.admin

/** Where the Administration section of Settings leads. */
internal data class AdminNavigation(
    val onOpenMembers: () -> Unit,
    val onOpenGroups: () -> Unit,
    val onOpenAuditLog: () -> Unit,
)
