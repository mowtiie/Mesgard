package com.mowtiie.mesgard.ui.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R

private val ContentPadding = 24.dp
private val IllustrationMaxHeight = 200.dp
private val SpacingAfterIllustration = 24.dp
private val SpacingBetweenTexts = 8.dp

/**
 * Fills the available space with an illustration, a title and a short hint. The illustration only decorates, so
 * screen readers read the title and the hint.
 */
@Composable
internal fun EmptyState(
    @DrawableRes illustration: Int,
    @StringRes title: Int,
    @StringRes hint: Int,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(ContentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SpacingBetweenTexts),
        ) {
            Image(
                painter = painterResource(illustration),
                contentDescription = null,
                modifier = Modifier.heightIn(max = IllustrationMaxHeight).padding(bottom = SpacingAfterIllustration),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** The empty state of a search that matches nothing, shared by every list with a search. */
@Composable
internal fun NoResultsState(modifier: Modifier = Modifier) {
    EmptyState(R.drawable.illustration_search, R.string.search_no_results, R.string.search_no_results_hint, modifier)
}
