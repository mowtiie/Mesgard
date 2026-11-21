package com.mowtiie.mesgard.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val HeaderSide = 16.dp
private val HeaderTop = 24.dp
private val HeaderBottom = 4.dp

/** The title of a group of rows. The space above separates it from the group before. */
@Composable
internal fun SectionHeader(@StringRes title: Int, modifier: Modifier = Modifier) {
    SectionHeader(stringResource(title), modifier)
}

@Composable
internal fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = HeaderSide, end = HeaderSide, top = HeaderTop, bottom = HeaderBottom)
            .semantics { heading() },
    )
}
