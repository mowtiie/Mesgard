package com.mowtiie.mesgard.ui.admin.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.AdminGroup
import com.mowtiie.mesgard.domain.model.MAX_GROUP_DEPTH

private val LevelIndent = 16.dp
private val TitlePadding = 24.dp

/** A group is at level 1 at the top, so a group inside [row] is at level `depth + 2`. */
private fun GroupRow.canHoldAnotherLevel() = depth + 2 <= MAX_GROUP_DEPTH

/**
 * Chooses the group another group goes inside, or the top. Groups that would make the tree too deep, and those in
 * [excludedIds] (a group cannot go inside itself or inside a group it contains), cannot be chosen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ParentPickerSheet(
    groups: List<AdminGroup>,
    selectedId: String?,
    excludedIds: Set<String>,
    onSelected: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Text(
            text = stringResource(R.string.group_parent_pick),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = TitlePadding).semantics { heading() },
        )
        LazyColumn {
            item(key = "top") {
                PickerRow(
                    name = stringResource(R.string.group_parent_top),
                    depth = 0,
                    isSelected = selectedId == null,
                    isEnabled = true,
                    onClick = { onSelected(null) },
                )
            }
            items(flattenGroupTree(groups), key = { it.group.id }) { row ->
                PickerRow(
                    name = row.group.name,
                    depth = row.depth + 1,
                    isSelected = row.group.id == selectedId,
                    isEnabled = row.group.id !in excludedIds && row.canHoldAnotherLevel(),
                    onClick = { onSelected(row.group.id) },
                )
            }
        }
    }
}

@Composable
private fun PickerRow(name: String, depth: Int, isSelected: Boolean, isEnabled: Boolean, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.padding(start = LevelIndent * depth).clickable(enabled = isEnabled, onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = {
            Text(
                text = name,
                color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
            )
        },
        trailingContent = if (isSelected) {
            { Icon(Icons.Outlined.Check, contentDescription = null) }
        } else {
            null
        },
    )
}
