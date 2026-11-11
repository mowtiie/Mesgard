package com.mowtiie.mesgard.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mowtiie.mesgard.ui.chats.ConversationListRoute
import com.mowtiie.mesgard.ui.contacts.ContactsScreenRoute
import com.mowtiie.mesgard.ui.profile.ProfileScreen
import com.mowtiie.mesgard.ui.settings.SettingsScreenRoute

@Composable
internal fun MainScreen(
    onSignOut: () -> Unit,
    onConversationClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    RequestNotificationPermissionOnce()
    val backStackEntry by navController.currentBackStackEntryAsState()
    var chatsScrollToTopRequests by rememberSaveable { mutableIntStateOf(0) }
    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteItems = {
            MainDestination.entries.forEach { destination ->
                val selected = backStackEntry?.destination.isOn(destination)
                item(
                    selected = selected,
                    onClick = {
                        when {
                            !selected -> navController.navigateToTopLevel(destination)
                            destination == MainDestination.CHATS -> chatsScrollToTopRequests++
                        }
                    },
                    icon = { MainDestinationIcon(destination, selected) },
                    label = { Text(stringResource(destination.labelResource)) },
                )
            }
        },
    ) {
        NavHost(navController = navController, startDestination = ChatsRoute) {
            composable<ChatsRoute> {
                ConversationListRoute(
                    scrollToTopRequests = chatsScrollToTopRequests,
                    onConversationClick = onConversationClick,
                )
            }
            composable<ContactsRoute> { ContactsScreenRoute(onConversationStarted = onConversationClick) }
            composable<ProfileRoute> { ProfileScreen(onSignOut = onSignOut) }
            composable<SettingsRoute> { SettingsScreenRoute() }
        }
    }
}

private fun NavDestination?.isOn(destination: MainDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.route::class) } == true

private fun NavHostController.navigateToTopLevel(destination: MainDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
