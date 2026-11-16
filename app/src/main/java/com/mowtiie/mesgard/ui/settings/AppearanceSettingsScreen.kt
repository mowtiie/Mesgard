package com.mowtiie.mesgard.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mowtiie.mesgard.R

@Composable
internal fun AppearanceSettingsScreenRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    SettingsSubpage(title = R.string.settings_appearance, onBack = onBack, modifier = modifier) {
        AppearanceSection()
    }
}
