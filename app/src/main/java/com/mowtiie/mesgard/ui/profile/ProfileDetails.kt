package com.mowtiie.mesgard.ui.profile

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.ui.common.InitialAvatar

private val ProfileAvatarSize = 96.dp
private val AvatarRingPadding = 4.dp
private val ContentPadding = 16.dp
private val HeaderVerticalPadding = 32.dp
private val CardSpacing = 12.dp

/** The name is the most important thing on this screen, so it sits on a colored container at the top. */
@Composable
internal fun ProfileHeader(account: Account) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = ContentPadding, vertical = HeaderVerticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ContentPadding),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                InitialAvatar(
                    name = account.displayName,
                    size = ProfileAvatarSize,
                    modifier = Modifier.padding(AvatarRingPadding),
                )
            }
            Text(
                text = account.displayName,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun AccountDetails(account: Account) {
    Column(
        modifier = Modifier.padding(ContentPadding),
        verticalArrangement = Arrangement.spacedBy(CardSpacing),
    ) {
        if (account.memberships.isNotEmpty()) {
            SectionHeader(R.string.profile_organizations)
            account.memberships.forEach { MembershipCard(it) }
        }
        account.email?.let { email ->
            SectionHeader(R.string.profile_account)
            DetailsCard {
                ListItem(
                    colors = transparentListItemColors(),
                    leadingContent = { Icon(Icons.Default.Email, contentDescription = null) },
                    overlineContent = { Text(stringResource(R.string.profile_email)) },
                    headlineContent = { Text(email) },
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

@StringRes
internal fun OrgRole.labelResource(): Int = when (this) {
    OrgRole.ORG_ADMIN -> R.string.role_org_admin
    OrgRole.STAFF -> R.string.role_staff
    OrgRole.STUDENT -> R.string.role_student
    OrgRole.OTHER -> R.string.role_member
}
