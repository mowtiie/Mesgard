package com.mowtiie.mesgard.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
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
internal fun MainDestinationIcon(destination: MainDestination) {
    when (destination) {
        MainDestination.CHATS -> Icon(painterResource(R.drawable.ic_chat), contentDescription = null)
        MainDestination.CONTACTS -> Icon(painterResource(R.drawable.ic_group), contentDescription = null)
        MainDestination.PROFILE -> Icon(Icons.Filled.Person, contentDescription = null)
        MainDestination.SETTINGS -> Icon(Icons.Filled.Settings, contentDescription = null)
    }
}
