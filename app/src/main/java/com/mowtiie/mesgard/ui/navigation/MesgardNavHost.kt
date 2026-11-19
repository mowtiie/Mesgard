package com.mowtiie.mesgard.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.mowtiie.mesgard.ui.about.AboutScreenRoute
import com.mowtiie.mesgard.ui.about.OpenSourceLicensesScreenRoute
import com.mowtiie.mesgard.ui.account.AccountScreenRoute
import com.mowtiie.mesgard.ui.account.ChangePasswordScreenRoute
import com.mowtiie.mesgard.ui.admin.AddMemberScreenRoute
import com.mowtiie.mesgard.ui.admin.AdminNavigation
import com.mowtiie.mesgard.ui.admin.AdministrationScreenRoute
import com.mowtiie.mesgard.ui.admin.ImportMembersScreenRoute
import com.mowtiie.mesgard.ui.admin.MembersScreenRoute
import com.mowtiie.mesgard.ui.admin.audit.AuditLogScreenRoute
import com.mowtiie.mesgard.ui.admin.former.FormerMembersScreenRoute
import com.mowtiie.mesgard.ui.admin.groups.AddGroupMembersScreenRoute
import com.mowtiie.mesgard.ui.admin.groups.GroupDetailScreenRoute
import com.mowtiie.mesgard.ui.admin.groups.GroupsScreenRoute
import com.mowtiie.mesgard.ui.auth.MfaSetupScreenRoute
import com.mowtiie.mesgard.ui.chats.ArchivedScreenRoute
import com.mowtiie.mesgard.ui.conversation.ConversationScreenRoute
import com.mowtiie.mesgard.ui.settings.AppearanceSettingsScreenRoute
import com.mowtiie.mesgard.ui.settings.NotificationSettingsScreenRoute
import com.mowtiie.mesgard.ui.settings.SecuritySettingsScreenRoute
import com.mowtiie.mesgard.ui.settings.SettingsNavigation

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
                onOpenArchived = { navController.navigate(ArchivedRoute) },
                settingsNavigation = SettingsNavigation(
                    onOpenAccount = { navController.navigate(AccountRoute) },
                    onOpenAdministration = { navController.navigate(AdministrationRoute) },
                    onOpenAppearance = { navController.navigate(AppearanceSettingsRoute) },
                    onOpenNotifications = { navController.navigate(NotificationSettingsRoute) },
                    onOpenSecurity = { navController.navigate(SecuritySettingsRoute) },
                    onOpenAbout = { navController.navigate(AboutRoute) },
                ),
            )
        }
        composable<AccountRoute> {
            AccountScreenRoute(
                onBack = { navController.popBackStack() },
                onChangePassword = { navController.navigate(ChangePasswordRoute) },
                onSetUpMfa = { navController.navigate(MfaSetupRoute) },
                onSignOut = onSignOut,
            )
        }
        composable<MfaSetupRoute> {
            MfaSetupScreenRoute(onFinished = { navController.popBackStack() })
        }
        composable<ChangePasswordRoute> {
            ChangePasswordScreenRoute(onBack = { navController.popBackStack() })
        }
        composable<AppearanceSettingsRoute> {
            AppearanceSettingsScreenRoute(onBack = { navController.popBackStack() })
        }
        composable<NotificationSettingsRoute> {
            NotificationSettingsScreenRoute(onBack = { navController.popBackStack() })
        }
        composable<SecuritySettingsRoute> {
            SecuritySettingsScreenRoute(onBack = { navController.popBackStack() })
        }
        composable<AboutRoute> {
            AboutScreenRoute(
                onBack = { navController.popBackStack() },
                onOpenLicenses = { navController.navigate(OpenSourceLicensesRoute) },
            )
        }
        composable<OpenSourceLicensesRoute> {
            OpenSourceLicensesScreenRoute(onBack = { navController.popBackStack() })
        }
        composable<AdministrationRoute> {
            AdministrationScreenRoute(
                onBack = { navController.popBackStack() },
                navigation = AdminNavigation(
                    onOpenMembers = { navController.navigate(MembersRoute) },
                    onOpenGroups = { navController.navigate(GroupsRoute) },
                    onOpenFormerMembers = { navController.navigate(FormerMembersRoute) },
                    onOpenAuditLog = { navController.navigate(AuditLogRoute) },
                    onAddMember = { navController.navigate(AddMemberRoute) },
                    onImportMembers = { navController.navigate(ImportMembersRoute) },
                ),
            )
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
        composable<FormerMembersRoute> {
            FormerMembersScreenRoute(onBack = { navController.popBackStack() })
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
        composable<ArchivedRoute> {
            ArchivedScreenRoute(
                onBack = { navController.popBackStack() },
                onConversationClick = { conversationId -> navController.navigate(ConversationRoute(conversationId)) },
            )
        }
        composable<ConversationRoute> {
            ConversationScreenRoute(onBack = { navController.popBackStack() })
        }
    }
}
