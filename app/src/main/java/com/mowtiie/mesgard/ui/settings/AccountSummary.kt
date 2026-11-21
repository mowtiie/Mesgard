package com.mowtiie.mesgard.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.ui.common.InitialAvatar
import com.mowtiie.mesgard.ui.common.NavigationChevron
import com.mowtiie.mesgard.ui.common.labelResource

private val SummaryAvatarSize = 56.dp

/** The signed in person at the top of Settings. Tapping it opens the Account screen. */
@Composable
internal fun AccountSummary(
    onOpenAccount: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountSummaryViewModel = hiltViewModel(),
) {
    val account by viewModel.account.collectAsStateWithLifecycle()
    account?.let {
        ListItem(
            modifier = modifier.clickable(
                onClickLabel = stringResource(R.string.account_open),
                role = Role.Button,
                onClick = onOpenAccount,
            ),
            leadingContent = { InitialAvatar(it.displayName, size = SummaryAvatarSize) },
            headlineContent = { Text(it.displayName) },
            supportingContent = {
                Column {
                    it.email?.let { email -> Text(email) }
                    Text(organizationLine(it.memberships))
                }
            },
            trailingContent = { NavigationChevron() },
        )
        HorizontalDivider()
    }
}

/** The role and the organization of a person who belongs to one, otherwise how many they belong to. */
@Composable
private fun organizationLine(memberships: List<AccountMembership>): String = when (memberships.size) {
    0 -> stringResource(R.string.settings_account_no_organization)
    1 -> stringResource(
        R.string.settings_account_organization,
        stringResource(memberships.single().role.labelResource()),
        memberships.single().organizationName,
    )
    else -> pluralStringResource(R.plurals.settings_account_organizations, memberships.size, memberships.size)
}
