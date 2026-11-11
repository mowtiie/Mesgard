package com.mowtiie.mesgard.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

private val SectionHeaderPadding = 16.dp

internal data class RadioLabel(@StringRes val title: Int, @StringRes val summary: Int? = null)

@Composable
internal fun SectionHeader(@StringRes title: Int) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = SectionHeaderPadding,
            end = SectionHeaderPadding,
            top = SectionHeaderPadding,
        ),
    )
}

@Composable
internal fun SwitchRow(
    @StringRes title: Int,
    @StringRes summary: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        headlineContent = { Text(stringResource(title)) },
        supportingContent = { Text(stringResource(summary)) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
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
