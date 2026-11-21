package com.mowtiie.mesgard.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
internal data object MainRoute

@Serializable
internal data class ConversationRoute(val conversationId: String)

@Serializable
internal data object ArchivedRoute

@Serializable
internal data object NewChatRoute

@Serializable
internal data object AccountRoute

@Serializable
internal data object ChangePasswordRoute

@Serializable
internal data object MfaSetupRoute

@Serializable
internal data object AppearanceSettingsRoute

@Serializable
internal data object NotificationSettingsRoute

@Serializable
internal data object SecuritySettingsRoute

@Serializable
internal data object AboutRoute

@Serializable
internal data object OpenSourceLicensesRoute

@Serializable
internal data object SettingsRoute

@Serializable
internal data object AdministrationRoute

@Serializable
internal data object MembersRoute

@Serializable
internal data object AddMemberRoute

@Serializable
internal data object ImportMembersRoute

@Serializable
internal data object GroupsRoute

@Serializable
internal data class GroupRoute(val groupId: String)

@Serializable
internal data class AddGroupMembersRoute(val groupId: String)

@Serializable
internal data object AuditLogRoute

@Serializable
internal data object ReportsRoute

@Serializable
internal data class ReportRoute(val reportId: String)

@Serializable
internal data object ReportReviewersRoute

@Serializable
internal data object FormerMembersRoute
