package com.mowtiie.mesgard.ui.settings

import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.NotificationPreview
import com.mowtiie.mesgard.ui.common.SectionHeader
import org.unifiedpush.android.connector.UnifiedPush

private val PushButtonSpacing = 8.dp

@Composable
internal fun NotificationSettingsScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current as? FragmentActivity

    SettingsSubpage(
        title = R.string.settings_notifications_title,
        onBack = onBack,
        modifier = modifier,
        error = uiState.error,
        onErrorShown = viewModel::onErrorShown,
    ) {
        SectionHeader(R.string.settings_delivery)
        PushSetupRow(uiState.pushDistributorName) {
            if (UnifiedPush.getDistributors(context).isEmpty()) {
                viewModel.onPushDistributorUnavailable()
            } else {
                activity?.let {
                    UnifiedPush.tryPickDistributor(it) { chosen -> if (chosen) viewModel.onPushDistributorChosen() }
                }
            }
        }
        SectionHeader(R.string.settings_notifications)
        RadioGroup(
            options = NotificationPreview.entries.map {
                it to RadioLabel(it.labelResource(), it.summaryResource())
            },
            selected = uiState.settings.notificationPreview,
            onSelected = viewModel::onNotificationPreviewSelected,
        )
    }
}

/** Until a distributor app is chosen nothing arrives while Mesgard is closed, so the way to fix it is a button. */
@Composable
private fun PushSetupRow(distributorName: String?, onChoose: () -> Unit) {
    val isSetUp = distributorName != null
    ListItem(
        leadingContent = {
            if (isSetUp) {
                Icon(Icons.Outlined.Notifications, contentDescription = null)
            } else {
                Icon(painterResource(R.drawable.ic_notifications_off), contentDescription = null)
            }
        },
        headlineContent = { Text(stringResource(R.string.settings_push)) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(PushButtonSpacing)) {
                Text(
                    if (distributorName == null) {
                        stringResource(R.string.settings_push_not_set_up)
                    } else {
                        stringResource(R.string.settings_push_using, distributorName)
                    },
                )
                if (isSetUp) {
                    OutlinedButton(onClick = onChoose) { Text(stringResource(R.string.settings_push_change)) }
                } else {
                    FilledTonalButton(onClick = onChoose) { Text(stringResource(R.string.settings_push_choose_button)) }
                }
            }
        },
    )
}

@StringRes
private fun NotificationPreview.labelResource(): Int = when (this) {
    NotificationPreview.SENDER_AND_TEXT -> R.string.preview_sender_and_text
    NotificationPreview.SENDER_ONLY -> R.string.preview_sender_only
    NotificationPreview.HIDDEN -> R.string.preview_hidden
}

@StringRes
private fun NotificationPreview.summaryResource(): Int = when (this) {
    NotificationPreview.SENDER_AND_TEXT -> R.string.preview_sender_and_text_summary
    NotificationPreview.SENDER_ONLY -> R.string.preview_sender_only_summary
    NotificationPreview.HIDDEN -> R.string.preview_hidden_summary
}
