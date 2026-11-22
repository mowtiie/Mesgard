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
import com.mowtiie.mesgard.ui.admin.groups.GroupCreationScreenRoute
import com.mowtiie.mesgard.ui.admin.groups.GroupDetailScreenRoute
import com.mowtiie.mesgard.ui.admin.groups.GroupsScreenRoute
import com.mowtiie.mesgard.ui.admin.groups.NewGroupScreenRoute
import com.mowtiie.mesgard.ui.auth.MfaSetupScreenRoute
import com.mowtiie.mesgard.ui.chats.ArchivedScreenRoute
import com.mowtiie.mesgard.ui.contacts.ContactsScreenRoute
import com.mowtiie.mesgard.ui.conversation.ConversationScreenRoute
import com.mowtiie.mesgard.ui.legalhold.LegalHoldsScreenRoute
import com.mowtiie.mesgard.ui.messaging.MessagingRulesScreenRoute
import com.mowtiie.mesgard.ui.privacy.PrivacyNoticeScreenRoute
import com.mowtiie.mesgard.ui.privacy.PrivacyNoticeSettingsScreenRoute
import com.mowtiie.mesgard.ui.reports.ReportDetailScreenRoute
import com.mowtiie.mesgard.ui.reports.ReportReviewersScreenRoute
import com.mowtiie.mesgard.ui.reports.ReportsScreenRoute
import com.mowtiie.mesgard.ui.retention.RetentionScreenRoute
import com.mowtiie.mesgard.ui.settings.AppearanceSettingsScreenRoute
import com.mowtiie.mesgard.ui.settings.NotificationSettingsScreenRoute
import com.mowtiie.mesgard.ui.settings.SecuritySettingsScreenRoute
import com.mowtiie.mesgard.ui.settings.SettingsNavigation
import com.mowtiie.mesgard.ui.settings.SettingsScreenRoute

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
                onConversationClick = { conversationId -> navController.navigate(ConversationRoute(conversationId)) },
                onOpenArchived = { navController.navigate(ArchivedRoute) },
                onNewChat = { navController.navigate(NewChatRoute) },
                onNewGroup = { navController.navigate(NewGroupRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
            )
        }
        composable<SettingsRoute> {
            SettingsScreenRoute(
                navigation = SettingsNavigation(
                    onOpenAccount = { navController.navigate(AccountRoute) },
                    onOpenAdministration = { navController.navigate(AdministrationRoute) },
                    onOpenReports = { navController.navigate(ReportsRoute) },
                    onOpenAppearance = { navController.navigate(AppearanceSettingsRoute) },
                    onOpenNotifications = { navController.navigate(NotificationSettingsRoute) },
                    onOpenSecurity = { navController.navigate(SecuritySettingsRoute) },
                    onOpenPrivacyNotice = { navController.navigate(PrivacyNoticeRoute) },
                    onOpenAbout = { navController.navigate(AboutRoute) },
                ),
                onSignOut = onSignOut,
                onBack = { navController.popBackStack() },
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
                    onOpenReportReviewers = { navController.navigate(ReportReviewersRoute) },
                    onOpenPrivacyNoticeSettings = { navController.navigate(PrivacyNoticeSettingsRoute) },
                    onOpenLegalHolds = { navController.navigate(LegalHoldsRoute) },
                    onOpenRetention = { navController.navigate(RetentionRoute) },
                    onOpenGroupCreation = { navController.navigate(GroupCreationRoute) },
                    onOpenMessagingRules = { navController.navigate(MessagingRulesRoute) },
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
                onCreateGroup = { navController.navigate(NewGroupRoute) },
            )
        }
        composable<NewGroupRoute> {
            NewGroupScreenRoute(onBack = { navController.popBackStack() })
        }
        composable<GroupRoute> { entry ->
            val groupId = entry.toRoute<GroupRoute>().groupId
            GroupDetailScreenRoute(
                groupId = groupId,
                onBack = { navController.popBackStack() },
                onAddMembers = { navController.navigate(AddGroupMembersRoute(groupId)) },
                onLeft = { navController.popBackStack(MainRoute, inclusive = false) },
            )
        }
        composable<AddGroupMembersRoute> {
            AddGroupMembersScreenRoute(onBack = { navController.popBackStack() })
        }
        composable<FormerMembersRoute> {
            FormerMembersScreenRoute(onBack = { navController.popBackStack() })
        }
        composable<MessagingRulesRoute> {
            MessagingRulesScreenRoute(onBack = { navController.popBackStack() })
        }
        composable<GroupCreationRoute> {
            GroupCreationScreenRoute(onBack = { navController.popBackStack() })
        }
        composable<RetentionRoute> {
            RetentionScreenRoute(onBack = { navController.popBackStack() })
        }
        composable<LegalHoldsRoute> {
            LegalHoldsScreenRoute(onBack = { navController.popBackStack() })
        }
        composable<PrivacyNoticeRoute> {
            PrivacyNoticeScreenRoute(onBack = { navController.popBackStack() })
        }
        composable<PrivacyNoticeSettingsRoute> {
            PrivacyNoticeSettingsScreenRoute(onBack = { navController.popBackStack() })
        }
        composable<ReportsRoute> {
            ReportsScreenRoute(
                onBack = { navController.popBackStack() },
                onOpenReport = { navController.navigate(ReportRoute(it)) },
            )
        }
        composable<ReportRoute> {
            ReportDetailScreenRoute(onBack = { navController.popBackStack() })
        }
        composable<ReportReviewersRoute> {
            ReportReviewersScreenRoute(onBack = { navController.popBackStack() })
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
        composable<NewChatRoute> {
            ContactsScreenRoute(
                onConversationStarted = { conversationId ->
                    navController.navigate(ConversationRoute(conversationId)) {
                        popUpTo<NewChatRoute> { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable<ConversationRoute> {
            ConversationScreenRoute(
                onBack = { navController.popBackStack() },
                onOpenGroup = { navController.navigate(GroupRoute(it)) },
            )
        }
    }
}
