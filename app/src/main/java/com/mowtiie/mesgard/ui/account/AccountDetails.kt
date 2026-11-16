package com.mowtiie.mesgard.ui.account

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.mowtiie.mesgard.ui.common.labelResource

private val AccountAvatarSize = 96.dp
private val AvatarRingPadding = 4.dp
private val ContentPadding = 16.dp
private val HeaderVerticalPadding = 32.dp
private val CardSpacing = 12.dp

/** The name is the most important thing on this screen, so it sits on a colored container at the top. */
@Composable
internal fun AccountHeader(account: Account) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = ContentPadding, vertical = HeaderVerticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ContentPadding),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                InitialAvatar(
                    name = account.displayName,
                    size = AccountAvatarSize,
                    modifier = Modifier.padding(AvatarRingPadding),
                )
            }
            Text(
                text = account.displayName,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
        }
    }
}

@Composable
internal fun AccountDetails(account: Account, onChangePassword: () -> Unit) {
    Column(
        modifier = Modifier.padding(ContentPadding),
        verticalArrangement = Arrangement.spacedBy(CardSpacing),
    ) {
        if (account.memberships.isNotEmpty()) {
            SectionHeader(R.string.account_organizations)
            account.memberships.forEach { MembershipCard(it) }
        }
        SectionHeader(R.string.account_details)
        DetailsCard {
            Column {
                account.email?.let { email ->
                    ListItem(
                        colors = transparentListItemColors(),
                        leadingContent = { Icon(Icons.Default.Email, contentDescription = null) },
                        overlineContent = { Text(stringResource(R.string.account_email)) },
                        headlineContent = { Text(email) },
                    )
                }
                ListItem(
                    modifier = Modifier.clickable(role = Role.Button, onClick = onChangePassword),
                    colors = transparentListItemColors(),
                    leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.account_change_password)) },
                )
            }
        }
    }
}

@Composable
private fun MembershipCard(membership: AccountMembership) {
    DetailsCard {
        ListItem(
            colors = transparentListItemColors(),
            headlineContent = {
                Text(text = membership.organizationName, style = MaterialTheme.typography.titleMedium)
            },
            trailingContent = { RoleBadge(membership.role) },
        )
    }
}

@Composable
private fun DetailsCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) { content() }
}

@Composable
private fun transparentListItemColors() = ListItemDefaults.colors(containerColor = Color.Transparent)

@Composable
private fun SectionHeader(@StringRes title: Int) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun RoleBadge(role: OrgRole) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(
            text = stringResource(role.labelResource()),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = ContentPadding, vertical = CardSpacing / 2),
        )
    }
}
