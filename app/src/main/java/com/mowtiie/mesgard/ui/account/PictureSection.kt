package com.mowtiie.mesgard.ui.account

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.ui.common.NavigationChevron

/** A row for each organization where the person has a profile picture or may set one. */
@Composable
internal fun PictureSection(
    memberships: List<AccountMembership>,
    onOpen: (String) -> Unit,
    viewModel: PictureEntryViewModel = hiltViewModel(),
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    LaunchedEffect(memberships) { viewModel.onMembershipsChanged(memberships) }
    val showsOrganization = rows.size > 1
    rows.forEach { row ->
        ListItem(
            modifier = Modifier.clickable(role = Role.Button) { onOpen(row.rules.orgId) },
            leadingContent = { Icon(Icons.Outlined.AccountCircle, contentDescription = null) },
            overlineContent = if (showsOrganization) ({ Text(row.organizationName) }) else null,
            headlineContent = { Text(stringResource(R.string.profile_picture_title)) },
            supportingContent = { Text(stringResource(row.supportingTextResource())) },
            trailingContent = { NavigationChevron() },
        )
    }
}

private fun PictureRow.supportingTextResource(): Int =
    if (rules.version != null) R.string.profile_picture_row_set else R.string.profile_picture_row_none
