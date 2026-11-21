package com.mowtiie.mesgard.ui.admin

/** Where the Administration page leads. */
internal data class AdminNavigation(
    val onOpenMembers: () -> Unit,
    val onOpenGroups: () -> Unit,
    val onOpenFormerMembers: () -> Unit,
    val onOpenAuditLog: () -> Unit,
    val onOpenReportReviewers: () -> Unit,
    val onOpenPrivacyNoticeSettings: () -> Unit,
    val onAddMember: () -> Unit,
    val onImportMembers: () -> Unit,
)
