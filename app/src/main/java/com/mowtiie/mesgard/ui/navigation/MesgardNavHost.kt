package com.mowtiie.mesgard.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mowtiie.mesgard.ui.conversation.ConversationScreenRoute

@Composable
internal fun MesgardNavHost(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = MainRoute, modifier = modifier) {
        composable<MainRoute> {
            MainScreen(
                onSignOut = onSignOut,
                onConversationClick = { conversationId -> navController.navigate(ConversationRoute(conversationId)) },
            )
        }
        composable<ConversationRoute> {
            ConversationScreenRoute(onBack = { navController.popBackStack() })
        }
    }
}
