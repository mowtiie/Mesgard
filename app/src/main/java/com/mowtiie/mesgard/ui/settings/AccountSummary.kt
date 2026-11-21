package com.mowtiie.mesgard.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.common.InitialAvatar
import com.mowtiie.mesgard.ui.common.NavigationChevron

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
            supportingContent = it.email?.let { email -> { Text(email) } },
            trailingContent = { NavigationChevron() },
        )
        HorizontalDivider()
    }
}
