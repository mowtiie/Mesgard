package com.mowtiie.mesgard.ui.admin.groups

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.GroupSettings
import com.mowtiie.mesgard.domain.model.PostingPolicy

/** Who can post and who is in the group. The whole row toggles, as Material guidance asks for switches. */
@Composable
internal fun GroupSettingsRows(
    settings: GroupSettings,
    enabled: Boolean,
    onChanged: (GroupSettings) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color = ListItemDefaults.containerColor,
    canIncludeEveryone: Boolean = true,
) {
    Column(modifier = modifier) {
        SettingRow(
            headline = R.string.group_setting_moderators_only,
            supporting = R.string.group_setting_moderators_only_hint,
            checked = settings.postingPolicy == PostingPolicy.MODERATORS,
            enabled = enabled,
            containerColor = containerColor,
        ) { onChanged(settings.copy(postingPolicy = if (it) PostingPolicy.MODERATORS else PostingPolicy.EVERYONE)) }
        if (canIncludeEveryone) {
            SettingRow(
                headline = R.string.group_setting_everyone,
                supporting = R.string.group_setting_everyone_hint,
                checked = settings.includesEveryone,
                enabled = enabled,
                containerColor = containerColor,
            ) { onChanged(settings.copy(includesEveryone = it)) }
        }
    }
}

@Composable
private fun SettingRow(
    headline: Int,
    supporting: Int,
    checked: Boolean,
    enabled: Boolean,
    containerColor: androidx.compose.ui.graphics.Color,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        modifier = Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
        colors = ListItemDefaults.colors(containerColor = containerColor),
        headlineContent = { Text(stringResource(headline)) },
        supportingContent = { Text(stringResource(supporting)) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
    )
}
