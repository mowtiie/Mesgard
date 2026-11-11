package com.mowtiie.mesgard.ui.settings

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.ContrastLevel
import com.mowtiie.mesgard.domain.model.ThemeMode
import com.mowtiie.mesgard.ui.theme.IsDynamicColorSupported

/** Dynamic color has no contrast levels, so the contrast choice is disabled while it is on. */
@Composable
internal fun AppearanceSection(viewModel: AppearanceViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isDynamicColorOn = settings.dynamicColor && IsDynamicColorSupported
    SectionHeader(R.string.settings_theme)
    RadioGroup(
        options = ThemeMode.entries.map { it to RadioLabel(it.labelResource()) },
        selected = settings.themeMode,
        onSelected = viewModel::onThemeModeSelected,
    )
    if (IsDynamicColorSupported) {
        SwitchRow(
            title = R.string.settings_dynamic_color,
            summary = R.string.settings_dynamic_color_summary,
            checked = settings.dynamicColor,
            onCheckedChange = viewModel::onDynamicColorChanged,
        )
    }
    SectionHeader(R.string.settings_contrast)
    RadioGroup(
        options = ContrastLevel.entries.map { it to RadioLabel(it.labelResource()) },
        selected = settings.contrast,
        onSelected = viewModel::onContrastSelected,
        enabled = !isDynamicColorOn,
    )
}

@StringRes
private fun ThemeMode.labelResource(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

@StringRes
private fun ContrastLevel.labelResource(): Int = when (this) {
    ContrastLevel.STANDARD -> R.string.contrast_standard
    ContrastLevel.MEDIUM -> R.string.contrast_medium
    ContrastLevel.HIGH -> R.string.contrast_high
}
