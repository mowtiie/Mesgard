package com.mowtiie.mesgard.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R

private val LogoSize = 72.dp
private val LogoIconSize = 36.dp
private val HeaderSpacing = 8.dp
private const val COMPACT_HEIGHT_DP = 480

/** True while the keyboard is open or the window is short, so a screen can drop what it does not need. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun rememberIsCompact(): Boolean =
    WindowInsets.isImeVisible || LocalConfiguration.current.screenHeightDp < COMPACT_HEIGHT_DP

/**
 * The brand at the top of the screens before sign in: a mark, the name, the tagline and, when given, what the screen
 * is for. The mark and the tagline give way when space is short, so the fields stay in view.
 */
@Composable
internal fun AuthHeader(@StringRes headline: Int? = null, modifier: Modifier = Modifier) {
    val isCompact = rememberIsCompact()
    Column(
        modifier = modifier.semantics(mergeDescendants = true) { heading() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HeaderSpacing),
    ) {
        if (!isCompact) BrandMark()
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Serif),
            color = MaterialTheme.colorScheme.primary,
        )
        if (!isCompact) {
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (headline != null) {
            Text(
                text = stringResource(headline),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun BrandMark() {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(LogoSize),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_chat),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(LogoIconSize),
            )
        }
    }
}
