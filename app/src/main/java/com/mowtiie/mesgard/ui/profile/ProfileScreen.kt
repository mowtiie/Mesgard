package com.mowtiie.mesgard.ui.profile

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.ui.common.InitialAvatar
import com.mowtiie.mesgard.ui.common.TitledScreen
import com.mowtiie.mesgard.ui.theme.MesgardTheme

private val ProfileAvatarSize = 88.dp
private val ContentPadding = 16.dp

@Composable
internal fun ProfileScreenRoute(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(
        uiState = uiState,
        onSignOutClicked = viewModel::onSignOutClicked,
        onSignOutConfirmed = {
            viewModel.onSignOutDismissed()
            onSignOut()
        },
        onSignOutDismissed = viewModel::onSignOutDismissed,
        onErrorShown = viewModel::onErrorShown,
        modifier = modifier,
    )
}

@Composable
internal fun ProfileScreen(
    uiState: ProfileUiState,
    onSignOutClicked: () -> Unit,
    onSignOutConfirmed: () -> Unit,
    onSignOutDismissed: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = uiState.error?.let { stringResource(it.messageResource()) }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onErrorShown()
        }
    }

    TitledScreen(
        title = stringResource(R.string.tab_profile),
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            if (uiState.isRefreshing && uiState.account == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            uiState.account?.let { account -> AccountDetails(account) }
            OutlinedButton(
                onClick = onSignOutClicked,
                modifier = Modifier.fillMaxWidth().padding(ContentPadding),
            ) {
                Text(stringResource(R.string.sign_out_button))
            }
        }
    }

    if (uiState.showSignOutConfirmation) {
        SignOutDialog(onConfirm = onSignOutConfirmed, onDismiss = onSignOutDismissed)
    }
}

@Composable
private fun AccountDetails(account: Account) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(ContentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ContentPadding),
    ) {
        InitialAvatar(name = account.displayName, size = ProfileAvatarSize)
        Text(
            text = account.displayName,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
    }
    account.memberships.forEach { membership ->
        ListItem(
            overlineContent = { Text(stringResource(R.string.profile_organization)) },
            headlineContent = { Text(membership.organizationName) },
            supportingContent = { Text(stringResource(membership.role.labelResource())) },
        )
    }
    account.email?.let { email ->
        ListItem(
            overlineContent = { Text(stringResource(R.string.profile_email)) },
            headlineContent = { Text(email) },
        )
    }
}

@Composable
private fun SignOutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sign_out_dialog_title)) },
        text = { Text(stringResource(R.string.sign_out_dialog_message)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.sign_out_button)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@StringRes
private fun OrgRole.labelResource(): Int = when (this) {
    OrgRole.ORG_ADMIN -> R.string.role_org_admin
    OrgRole.STAFF -> R.string.role_staff
    OrgRole.STUDENT -> R.string.role_student
    OrgRole.OTHER -> R.string.role_member
}

@StringRes
private fun ProfileError.messageResource(): Int = when (this) {
    ProfileError.REFRESH_NETWORK_UNAVAILABLE -> R.string.refresh_error_network_unavailable
    ProfileError.REFRESH_UNKNOWN -> R.string.refresh_error_unknown
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    MesgardTheme {
        ProfileScreen(
            uiState = ProfileUiState(
                account = Account(
                    displayName = "Staff One",
                    email = "staff1@rnhs.test",
                    memberships = listOf(AccountMembership("org", "Rizal National High School", OrgRole.STAFF)),
                ),
            ),
            onSignOutClicked = {},
            onSignOutConfirmed = {},
            onSignOutDismissed = {},
            onErrorShown = {},
        )
    }
}
