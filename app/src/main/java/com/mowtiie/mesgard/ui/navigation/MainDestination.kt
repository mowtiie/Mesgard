package com.mowtiie.mesgard.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.mowtiie.mesgard.R

internal enum class MainDestination(val route: Any, @StringRes val labelResource: Int) {
    CHATS(ChatsRoute, R.string.tab_chats),
    CONTACTS(ContactsRoute, R.string.tab_contacts),
    PROFILE(ProfileRoute, R.string.tab_profile),
    SETTINGS(SettingsRoute, R.string.tab_settings),
}

@Composable
internal fun MainDestinationIcon(destination: MainDestination, selected: Boolean) {
    when (destination) {
        MainDestination.CHATS -> Icon(
            painter = painterResource(if (selected) R.drawable.ic_chat else R.drawable.ic_chat_outline),
            contentDescription = null,
        )
        MainDestination.CONTACTS -> Icon(
            painter = painterResource(if (selected) R.drawable.ic_group else R.drawable.ic_group_outline),
            contentDescription = null,
        )
        MainDestination.PROFILE -> Icon(
            imageVector = if (selected) Icons.Filled.Person else Icons.Outlined.Person,
            contentDescription = null,
        )
        MainDestination.SETTINGS -> Icon(
            imageVector = if (selected) Icons.Filled.Settings else Icons.Outlined.Settings,
            contentDescription = null,
        )
    }
}
