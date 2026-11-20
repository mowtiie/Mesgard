package com.mowtiie.mesgard.ui.chats

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

private val RowPadding = 16.dp
private val ChipSpacing = 8.dp

/**
 * A single choice filter, as the filter chips page of Material 3 allows: the chip you tapped shows a check, the set
 * scrolls sideways when the labels do not fit, and the chips are 8 dp apart.
 */
@Composable
internal fun ConversationFilterRow(
    selected: ConversationFilter,
    onSelected: (ConversationFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = RowPadding),
        horizontalArrangement = Arrangement.spacedBy(ChipSpacing),
    ) {
        ConversationFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelected(filter) },
                label = { Text(stringResource(filter.label)) },
                leadingIcon = if (filter == selected) {
                    { Icon(Icons.Outlined.Done, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) }
                } else {
                    null
                },
            )
        }
    }
}
