package com.mowtiie.mesgard.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.common.TitledScreen
import com.mowtiie.mesgard.ui.profile.ProfileScreen

@Composable
internal fun MesgardNavHost(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteItems = {
            MainDestination.entries.forEach { destination ->
                item(
                    selected = backStackEntry?.destination.isOn(destination),
                    onClick = { navController.navigateToTopLevel(destination) },
                    icon = { MainDestinationIcon(destination) },
                    label = { Text(stringResource(destination.labelResource)) },
                )
            }
        },
    ) {
        NavHost(navController = navController, startDestination = ChatsRoute) {
            composable<ChatsRoute> { TitledScreen(stringResource(R.string.tab_chats)) {} }
            composable<ContactsRoute> { TitledScreen(stringResource(R.string.tab_contacts)) {} }
            composable<ProfileRoute> { ProfileScreen(onSignOut = onSignOut) }
            composable<SettingsRoute> { TitledScreen(stringResource(R.string.tab_settings)) {} }
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
