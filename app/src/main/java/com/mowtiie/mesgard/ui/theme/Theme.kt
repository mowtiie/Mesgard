package com.mowtiie.mesgard.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.mowtiie.mesgard.domain.model.ContrastLevel
import com.mowtiie.mesgard.domain.model.ThemeMode

/** Dynamic color needs Android 12. It has no contrast levels, so the contrast choice is ignored while it is on. */
internal val IsDynamicColorSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

internal fun colorSchemeFor(darkTheme: Boolean, contrast: ContrastLevel): ColorScheme = when (contrast) {
    ContrastLevel.STANDARD -> if (darkTheme) DarkColorScheme else LightColorScheme
    ContrastLevel.MEDIUM -> if (darkTheme) MediumContrastDarkColorScheme else MediumContrastLightColorScheme
    ContrastLevel.HIGH -> if (darkTheme) HighContrastDarkColorScheme else HighContrastLightColorScheme
}

internal fun ThemeMode.isDark(systemInDarkTheme: Boolean): Boolean = when (this) {
    ThemeMode.SYSTEM -> systemInDarkTheme
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun MesgardTheme(content: @Composable () -> Unit) {
    MesgardTheme(
        darkTheme = isSystemInDarkTheme(),
        contrast = ContrastLevel.STANDARD,
        dynamicColor = false,
        content = content,
    )
}

@Composable
internal fun MesgardTheme(
    darkTheme: Boolean,
    contrast: ContrastLevel,
    dynamicColor: Boolean,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && IsDynamicColorSupported ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> colorSchemeFor(darkTheme, contrast)
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
