package com.mowtiie.mesgard.ui.account

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.ui.common.ScrollingPage
import com.mowtiie.mesgard.ui.common.TitledScreen
import com.mowtiie.mesgard.ui.theme.MesgardTheme

private val ContentPadding = 8.dp

@Composable
internal fun AccountScreenRoute(
    onBack: () -> Unit,
    onChangePassword: () -> Unit,
    onSetUpMfa: () -> Unit,
    onOpenProfilePicture: (String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AccountScreen(
        uiState = uiState,
        onBack = onBack,
        onChangePassword = onChangePassword,
        onMfaClicked = { if (uiState.mfa == MfaRowState.OFF) onSetUpMfa() else viewModel.onDisableMfaClicked() },
        onDisableMfaConfirmed = viewModel::onDisableMfaConfirmed,
        onDisableMfaDismissed = viewModel::onDisableMfaDismissed,
        onSignOutClicked = viewModel::onSignOutClicked,
        onSignOutConfirmed = {
            viewModel.onSignOutDismissed()
            onSignOut()
        },
        onSignOutDismissed = viewModel::onSignOutDismissed,
        onErrorShown = viewModel::onErrorShown,
        modifier = modifier,
        profileSections = { account ->
            PictureSection(account.memberships, onOpenProfilePicture)
            UsernameSection(account.memberships)
        },
    )
}

@Composable
internal fun AccountScreen(
    uiState: AccountUiState,
    onBack: () -> Unit,
    onChangePassword: () -> Unit,
    onMfaClicked: () -> Unit,
    onDisableMfaConfirmed: () -> Unit,
    onDisableMfaDismissed: () -> Unit,
    onSignOutClicked: () -> Unit,
    onSignOutConfirmed: () -> Unit,
    onSignOutDismissed: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
    profileSections: @Composable (Account) -> Unit = {},
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
        title = stringResource(R.string.account_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        ScrollingPage(padding) {
            if (uiState.isRefreshing && uiState.account == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            uiState.account?.let { account ->
                AccountHeader(account)
                profileSections(account)
                AccountDetails(account, uiState.mfa, onChangePassword, onMfaClicked)
            }
            HorizontalDivider(modifier = Modifier.padding(top = ContentPadding))
            SignOutRow(onSignOutClicked)
        }
    }

    if (uiState.showDisableMfaConfirmation) {
        DisableMfaDialog(
            isWorking = uiState.isWorking,
            onConfirm = onDisableMfaConfirmed,
            onDismiss = onDisableMfaDismissed,
        )
    }
    if (uiState.showSignOutConfirmation) {
        SignOutDialog(onConfirm = onSignOutConfirmed, onDismiss = onSignOutDismissed)
    }
}

@StringRes
private fun AccountError.messageResource(): Int = when (this) {
    AccountError.REFRESH_NETWORK_UNAVAILABLE -> R.string.refresh_error_network_unavailable
    AccountError.REFRESH_UNKNOWN -> R.string.refresh_error_unknown
    AccountError.MFA_NETWORK_UNAVAILABLE -> R.string.account_mfa_error_network_unavailable
    AccountError.MFA_UNKNOWN -> R.string.account_mfa_error_unknown
}

@Preview(showBackground = true)
@Composable
private fun AccountScreenPreview() {
    MesgardTheme {
        AccountScreen(
            uiState = AccountUiState(
                account = Account(
                    displayName = "Staff One",
                    email = "staff1@rnhs.test",
                    memberships = listOf(AccountMembership("org", "Rizal National High School", OrgRole.STAFF)),
                ),
            ),
            onBack = {},
            onChangePassword = {},
            onMfaClicked = {},
            onDisableMfaConfirmed = {},
            onDisableMfaDismissed = {},
            onSignOutClicked = {},
            onSignOutConfirmed = {},
            onSignOutDismissed = {},
            onErrorShown = {},
        )
    }
}
