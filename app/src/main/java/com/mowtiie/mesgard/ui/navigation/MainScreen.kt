package com.mowtiie.mesgard.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mowtiie.mesgard.ui.chats.ConversationListRoute

/** The home screen is the conversation list; everything else opens above it. */
@Composable
internal fun MainScreen(
    onConversationClick: (String) -> Unit,
    onOpenArchived: () -> Unit,
    onNewChat: () -> Unit,
    onNewGroup: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RequestNotificationPermissionOnce()
    ConversationListRoute(
        onConversationClick = onConversationClick,
        onOpenArchived = onOpenArchived,
        onNewChat = onNewChat,
        onNewGroup = onNewGroup,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
    )
}
