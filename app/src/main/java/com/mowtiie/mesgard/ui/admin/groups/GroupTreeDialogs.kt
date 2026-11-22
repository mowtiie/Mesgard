package com.mowtiie.mesgard.ui.admin.groups

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.GroupKind
import com.mowtiie.mesgard.ui.common.FormSheet
import com.mowtiie.mesgard.ui.common.NoteField
import com.mowtiie.mesgard.ui.settings.RadioGroup
import com.mowtiie.mesgard.ui.settings.RadioLabel

private const val MAX_MESSAGE_LENGTH = 4000

/** Asks why, because opening the messages of a private group is recorded and the members can see that it happened. */
@Composable
internal fun OpenMessagesSheet(isOpening: Boolean, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var reason by rememberSaveable { mutableStateOf("") }
    FormSheet(
        title = stringResource(R.string.group_open_messages_title),
        confirmLabel = stringResource(R.string.group_open_messages_confirm),
        onConfirm = { onConfirm(reason) },
        onDismiss = onDismiss,
        confirmEnabled = !isOpening && reason.trim().length >= ACCESS_REASON_MIN_LENGTH,
        dismissEnabled = !isOpening,
    ) {
        Text(
            text = stringResource(R.string.group_open_messages_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NoteField(
            value = reason,
            onValueChange = { reason = it },
            label = R.string.legal_hold_reason_label,
            helper = stringResource(R.string.legal_hold_reason_helper, ACCESS_REASON_MIN_LENGTH),
            maxLength = ACCESS_REASON_MAX_LENGTH,
            enabled = !isOpening,
        )
    }
}

/** The line under the title of a group: its kind, and the group it is inside when it is inside one. */
@Composable
internal fun placeSummary(tree: GroupTreeUiState): String? {
    val kind = tree.kind?.let { stringResource(it.labelResource()) }
    val parentName = tree.parentName
    return when {
        kind == null -> null
        parentName == null -> kind
        else -> stringResource(R.string.group_place_inside, kind, parentName)
    }
}

/** The sheets that change a group's kind or move it. Only administrators can open them. */
@Composable
internal fun GroupTreeDialogs(tree: GroupTreeUiState, viewModel: GroupTreeViewModel) {
    when (tree.dialog) {
        GroupTreeDialog.KIND -> KindSheet(tree, viewModel)
        GroupTreeDialog.ANNOUNCE -> AnnounceSheet(tree, viewModel)
        GroupTreeDialog.PARENT -> ParentPickerSheet(
            groups = tree.groups,
            selectedId = tree.parentId,
            excludedIds = tree.excludedParentIds,
            onSelected = viewModel::onParentChosen,
            onDismiss = { viewModel.onDialogChanged(null) },
        )
        null -> Unit
    }
}

/** Sends one text message to the group and every group inside it. Each group gets it in its own conversation. */
@Composable
private fun AnnounceSheet(tree: GroupTreeUiState, viewModel: GroupTreeViewModel) {
    var body by rememberSaveable { mutableStateOf("") }
    FormSheet(
        title = stringResource(R.string.group_announce_title),
        confirmLabel = stringResource(R.string.group_announce_send),
        onConfirm = { viewModel.onAnnounceConfirmed(body) },
        onDismiss = { viewModel.onDialogChanged(null) },
        confirmEnabled = !tree.isWorking && body.isNotBlank(),
        dismissEnabled = !tree.isWorking,
    ) {
        NoteField(
            value = body,
            onValueChange = { body = it },
            label = R.string.group_announce_label,
            helper = stringResource(R.string.group_announce_helper),
            maxLength = MAX_MESSAGE_LENGTH,
            minLines = 3,
            enabled = !tree.isWorking,
        )
    }
}

@Composable
private fun KindSheet(tree: GroupTreeUiState, viewModel: GroupTreeViewModel) {
    var selected by remember { mutableStateOf(tree.kind ?: GroupKind.OTHER) }
    FormSheet(
        title = stringResource(R.string.group_change_kind),
        confirmLabel = stringResource(R.string.group_change_kind_save),
        onConfirm = { viewModel.onKindChosen(selected) },
        onDismiss = { viewModel.onDialogChanged(null) },
        confirmEnabled = !tree.isWorking && selected != tree.kind,
    ) {
        RadioGroup(
            options = GroupKind.entries.map { it to RadioLabel(it.labelResource()) },
            selected = selected,
            onSelected = { selected = it },
            enabled = !tree.isWorking,
        )
    }
}
