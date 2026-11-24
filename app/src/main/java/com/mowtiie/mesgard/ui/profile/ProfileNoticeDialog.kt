package com.mowtiie.mesgard.ui.profile

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.repository.ProfileNoticeKind

/**
 * Tells the person once that a reviewer changed something of theirs. The text is neutral: it names no reporter and no
 * report. The view model is keyed by user id, so the next person on this phone never sees the last one's notice.
 */
@Composable
internal fun ProfileNoticeDialog(userId: String, viewModel: ProfileNoticeViewModel = hiltViewModel(key = userId)) {
    val notices by viewModel.notices.collectAsStateWithLifecycle()
    val notice = notices.firstOrNull() ?: return
    AlertDialog(
        onDismissRequest = viewModel::onAcknowledged,
        title = { Text(stringResource(R.string.profile_notice_title)) },
        text = {
            val text = when (notice.kind) {
                ProfileNoticeKind.USERNAME_RESET -> R.string.profile_notice_username_reset
                ProfileNoticeKind.OTHER -> R.string.profile_notice_other
            }
            Text(stringResource(text))
        },
        confirmButton = {
            TextButton(onClick = viewModel::onAcknowledged) { Text(stringResource(R.string.profile_notice_ok)) }
        },
    )
}
