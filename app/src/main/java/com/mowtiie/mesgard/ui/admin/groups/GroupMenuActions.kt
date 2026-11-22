package com.mowtiie.mesgard.ui.admin.groups

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.common.OverflowMenu

/** What the menu of a group does. Each entry closes the menu first. */
internal class GroupMenuActions(
    val onAnnounce: () -> Unit,
    val onChangeKind: () -> Unit,
    val onMove: () -> Unit,
    val onLegalHold: () -> Unit,
    val onLeave: () -> Unit,
    val onReadMessages: () -> Unit,
)

/** The overflow menu of the group screen: what the person may do depends on being an administrator or a member. */
@Composable
internal fun GroupMenu(
    isAdmin: Boolean,
    isMember: Boolean,
    canAnnounce: Boolean,
    canReadMessages: Boolean,
    actions: GroupMenuActions,
) {
    if (!isAdmin && !isMember) return
    OverflowMenu { dismiss ->
        @Composable
        fun item(text: Int, onClick: () -> Unit) = DropdownMenuItem(
            text = { Text(stringResource(text)) },
            onClick = {
                dismiss()
                onClick()
            },
        )
        if (canAnnounce) item(R.string.group_announce, actions.onAnnounce)
        if (isAdmin) {
            item(R.string.group_change_kind, actions.onChangeKind)
            item(R.string.group_move, actions.onMove)
            item(R.string.legal_hold_menu, actions.onLegalHold)
        }
        if (canReadMessages) item(R.string.group_read_messages, actions.onReadMessages)
        if (isMember) item(R.string.group_leave, actions.onLeave)
    }
}
