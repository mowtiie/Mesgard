package com.mowtiie.mesgard.ui.settings

/** Where the rows of the Settings home lead. */
internal data class SettingsNavigation(
    val onOpenAccount: () -> Unit,
    val onOpenAdministration: () -> Unit,
    val onOpenReports: () -> Unit,
    val onOpenAppearance: () -> Unit,
    val onOpenNotifications: () -> Unit,
    val onOpenSecurity: () -> Unit,
    val onOpenPrivacyNotice: () -> Unit,
    val onOpenAbout: () -> Unit,
)
