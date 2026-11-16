package com.mowtiie.mesgard.ui.settings

/** Where the rows of the Settings home lead. */
internal data class SettingsNavigation(
    val onOpenAccount: () -> Unit,
    val onOpenAppearance: () -> Unit,
    val onOpenNotifications: () -> Unit,
    val onOpenSecurity: () -> Unit,
)
