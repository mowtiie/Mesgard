package com.mowtiie.mesgard.ui.admin.groups

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.GroupKind
import com.mowtiie.mesgard.ui.common.FormSheet
import com.mowtiie.mesgard.ui.settings.RadioGroup
import com.mowtiie.mesgard.ui.settings.RadioLabel

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
