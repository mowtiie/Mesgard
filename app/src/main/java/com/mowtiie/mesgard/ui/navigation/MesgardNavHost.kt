package com.mowtiie.mesgard.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.mowtiie.mesgard.ui.account.AccountScreenRoute
import com.mowtiie.mesgard.ui.account.ChangePasswordScreenRoute
import com.mowtiie.mesgard.ui.admin.AddMemberScreenRoute
import com.mowtiie.mesgard.ui.admin.AdminNavigation
import com.mowtiie.mesgard.ui.admin.ImportMembersScreenRoute
import com.mowtiie.mesgard.ui.admin.MembersScreenRoute
import com.mowtiie.mesgard.ui.admin.audit.AuditLogScreenRoute
import com.mowtiie.mesgard.ui.admin.groups.AddGroupMembersScreenRoute
import com.mowtiie.mesgard.ui.admin.groups.GroupDetailScreenRoute
import com.mowtiie.mesgard.ui.admin.groups.GroupsScreenRoute
import com.mowtiie.mesgard.ui.conversation.ConversationScreenRoute

@Composable
internal fun MesgardNavHost(
    onSignOut: () -> Unit,
    conversationToOpen: String?,
    onConversationOpened: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    LaunchedEffect(conversationToOpen) {
        if (conversationToOpen != null) {
            navController.navigate(ConversationRoute(conversationToOpen))
            onConversationOpened()
        }
    }

    NavHost(navController = navController, startDestination = MainRoute, modifier = modifier) {
        composable<MainRoute> {
            MainScreen(
                onSignOut = onSignOut,
                onConversationClick = { conversationId -> navController.navigate(ConversationRoute(conversationId)) },
                onOpenAccount = { navController.navigate(AccountRoute) },
                adminNavigation = AdminNavigation(
                    onOpenMembers = { navController.navigate(MembersRoute) },
                    onOpenGroups = { navController.navigate(GroupsRoute) },
                    onOpenAuditLog = { navController.navigate(AuditLogRoute) },
                ),
            )
        }
        composable<AccountRoute> {
            AccountScreenRoute(
                onBack = { navController.popBackStack() },
                onChangePassword = { navController.navigate(ChangePasswordRoute) },
                onSignOut = onSignOut,
            )
        }
        composable<ChangePasswordRoute> {
            ChangePasswordScreenRoute(onBack = { navController.popBackStack() })
        }
        composable<MembersRoute> {
            MembersScreenRoute(
                onBack = { navController.popBackStack() },
                onAddMember = { navController.navigate(AddMemberRoute) },
                onImportMembers = { navController.navigate(ImportMembersRoute) },
            )
        }
        composable<GroupsRoute> {
            GroupsScreenRoute(
                onBack = { navController.popBackStack() },
                onOpenGroup = { navController.navigate(GroupRoute(it)) },
            )
        }
        composable<GroupRoute> { entry ->
            val groupId = entry.toRoute<GroupRoute>().groupId
            GroupDetailScreenRoute(
                onBack = { navController.popBackStack() },
                onAddMembers = { navController.navigate(AddGroupMembersRoute(groupId)) },
            )
        }
        composable<AddGroupMembersRoute> {
            AddGroupMembersScreenRoute(onBack = { navController.popBackStack() })
        }
        composable<AuditLogRoute> {
            AuditLogScreenRoute(onBack = { navController.popBackStack() })
        }
        composable<ImportMembersRoute> {
            ImportMembersScreenRoute(onBack = { navController.popBackStack() })
        }
        composable<AddMemberRoute> {
            AddMemberScreenRoute(onBack = { navController.popBackStack() })
        }
        composable<ConversationRoute> {
            ConversationScreenRoute(onBack = { navController.popBackStack() })
        }
    }
}
