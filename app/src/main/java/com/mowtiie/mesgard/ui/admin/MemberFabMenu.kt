package com.mowtiie.mesgard.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import com.mowtiie.mesgard.R

private const val HALF = 0.5f

/**
 * Opens from a regular FAB with the two ways to bring people in, as the Material 3 FAB menu guidance describes: two
 * to six related actions, each with an icon and a label, and the FAB turns into the close button.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MemberFabMenu(onAddMember: () -> Unit, onImportMembers: () -> Unit, modifier: Modifier = Modifier) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = isExpanded) { isExpanded = false }
    val toggleDescription = stringResource(R.string.members_fab_menu)
    val expandedDescription = stringResource(R.string.state_expanded)
    val collapsedDescription = stringResource(R.string.state_collapsed)

    FloatingActionButtonMenu(
        modifier = modifier,
        expanded = isExpanded,
        button = {
            ToggleFloatingActionButton(
                modifier = Modifier.semantics {
                    traversalIndex = -1f
                    contentDescription = toggleDescription
                    stateDescription = if (isExpanded) expandedDescription else collapsedDescription
                },
                checked = isExpanded,
                onCheckedChange = { isExpanded = it },
            ) {
                val icon by remember {
                    derivedStateOf { if (checkedProgress > HALF) Icons.Outlined.Close else Icons.Outlined.Add }
                }
                Icon(icon, contentDescription = null, modifier = Modifier.animateIcon({ checkedProgress }))
            }
        },
    ) {
        FloatingActionButtonMenuItem(
            onClick = {
                isExpanded = false
                onAddMember()
            },
            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.members_add)) },
        )
        FloatingActionButtonMenuItem(
            onClick = {
                isExpanded = false
                onImportMembers()
            },
            icon = { Icon(Icons.Outlined.Upload, contentDescription = null) },
            text = { Text(stringResource(R.string.members_import)) },
        )
    }
}
