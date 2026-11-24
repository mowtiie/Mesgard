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

/** Without [orgId], as in a conversation, the first organization the person belongs to is used. */
@Serializable
internal data class ProfileRoute(val userId: String, val orgId: String? = null)

@Serializable
internal data class ProfilePictureRoute(val orgId: String)

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
internal data class GroupMessagesRoute(val groupId: String)

@Serializable
internal data object NewGroupRoute

@Serializable
internal data class AddGroupMembersRoute(val groupId: String)

@Serializable
internal data object AuditLogRoute

@Serializable
internal data object MessagingRulesRoute

@Serializable
internal data object RetentionRoute

@Serializable
internal data object GroupCreationRoute

@Serializable
internal data object UsernamePolicyRoute

@Serializable
internal data object ImportGroupsRoute

@Serializable
internal data object LegalHoldsRoute

@Serializable
internal data object PrivacyNoticeRoute

@Serializable
internal data object PrivacyNoticeSettingsRoute

@Serializable
internal data object ReportsRoute

@Serializable
internal data class ReportRoute(val reportId: String)

@Serializable
internal data object ReportReviewersRoute

@Serializable
internal data object FormerMembersRoute
