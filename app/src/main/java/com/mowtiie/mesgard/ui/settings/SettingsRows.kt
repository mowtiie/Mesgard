package com.mowtiie.mesgard.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.mowtiie.mesgard.ui.common.NavigationChevron

@Composable
internal fun SwitchRow(
    @StringRes title: Int,
    @StringRes summary: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    ListItem(
        modifier = Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
        headlineContent = { Text(stringResource(title)) },
        supportingContent = { Text(stringResource(summary)) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
    )
}

@Composable
internal fun <T> RadioGroup(
    options: List<Pair<T, RadioLabel>>,
    selected: T,
    onSelected: (T) -> Unit,
    enabled: Boolean = true,
) {
    Column(modifier = Modifier.selectableGroup()) {
        options.forEach { (value, label) ->
            ListItem(
                modifier = Modifier.selectable(
                    selected = value == selected,
                    enabled = enabled,
                    role = Role.RadioButton,
                    onClick = { onSelected(value) },
                ),
                leadingContent = { RadioButton(selected = value == selected, onClick = null, enabled = enabled) },
                headlineContent = { Text(stringResource(label.title)) },
                supportingContent = label.summary?.let { summary -> { Text(stringResource(summary)) } },
            )
        }
    }
}

/** A row of the Settings home that opens a page. The summary says what is currently set. */
@Composable
internal fun SettingsCategoryRow(
    icon: @Composable () -> Unit,
    @StringRes title: Int,
    summary: String,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
        leadingContent = icon,
        headlineContent = { Text(stringResource(title)) },
        supportingContent = { Text(summary) },
        trailingContent = { NavigationChevron() },
    )
}
