package com.mowtiie.mesgard.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mowtiie.mesgard.ui.start.StartScreen

@Composable
internal fun MesgardNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = StartRoute,
        modifier = modifier,
    ) {
        composable<StartRoute> { StartScreen() }
    }
}
