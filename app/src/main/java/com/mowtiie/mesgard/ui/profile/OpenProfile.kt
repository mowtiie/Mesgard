package com.mowtiie.mesgard.ui.profile

import androidx.compose.runtime.compositionLocalOf

/** Opens the profile of a person of an organization. Provided where screens are navigated. */
internal val LocalOpenProfile = compositionLocalOf<(orgId: String?, userId: String) -> Unit> { { _, _ -> } }
