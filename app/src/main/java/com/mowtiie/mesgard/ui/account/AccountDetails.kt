package com.mowtiie.mesgard.ui.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.ui.common.InitialAvatar
import com.mowtiie.mesgard.ui.common.NavigationChevron
import com.mowtiie.mesgard.ui.common.SectionHeader
import com.mowtiie.mesgard.ui.common.labelResource

private val AccountAvatarSize = 72.dp
private val ContentPadding = 16.dp
private val HeaderTopPadding = 24.dp
private val BadgeVerticalPadding = 6.dp

/** The name is the most important thing on this screen: the avatar and the name, centered, with nothing around them. */
@Composable
internal fun AccountHeader(account: Account) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ContentPadding).padding(top = HeaderTopPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ContentPadding),
    ) {
        InitialAvatar(name = account.displayName, size = AccountAvatarSize)
        Text(
            text = account.displayName,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
    }
}

@Composable
internal fun AccountDetails(
    account: Account,
    mfa: MfaRowState,
    onChangePassword: () -> Unit,
    onMfaClicked: () -> Unit,
) {
    if (account.memberships.isNotEmpty()) {
        SectionHeader(R.string.account_organizations)
        account.memberships.forEach { MembershipRow(it) }
    }
    SectionHeader(R.string.account_sign_in_section)
    account.email?.let { email ->
        ListItem(
            leadingContent = { Icon(Icons.Outlined.Email, contentDescription = null) },
            overlineContent = { Text(stringResource(R.string.account_email)) },
            headlineContent = { Text(email) },
        )
    }
    ListItem(
        modifier = Modifier.clickable(role = Role.Button, onClick = onChangePassword),
        leadingContent = { Icon(Icons.Outlined.Lock, contentDescription = null) },
        headlineContent = { Text(stringResource(R.string.account_change_password)) },
        trailingContent = { NavigationChevron() },
    )
    MfaRow(mfa, onMfaClicked)
}

@Composable
private fun MfaRow(state: MfaRowState, onClick: () -> Unit) {
    val supporting = when (state) {
        MfaRowState.OFF -> R.string.account_mfa_off
        MfaRowState.ON -> R.string.account_mfa_on
        MfaRowState.REQUIRED -> R.string.account_mfa_required
    }
    val rowModifier = if (state == MfaRowState.REQUIRED) {
        Modifier
    } else {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    }
    ListItem(
        modifier = rowModifier,
        leadingContent = { Icon(Icons.Outlined.Shield, contentDescription = null) },
        headlineContent = { Text(stringResource(R.string.account_mfa)) },
        supportingContent = { Text(stringResource(supporting)) },
        trailingContent = if (state == MfaRowState.REQUIRED) null else ({ NavigationChevron() }),
    )
}

@Composable
private fun MembershipRow(membership: AccountMembership) {
    ListItem(
        headlineContent = { Text(membership.organizationName) },
        trailingContent = { RoleBadge(membership.role) },
    )
}

@Composable
private fun RoleBadge(role: OrgRole) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(
            text = stringResource(role.labelResource()),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = ContentPadding, vertical = BadgeVerticalPadding),
        )
    }
}
