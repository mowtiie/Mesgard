package com.mowtiie.mesgard.ui.privacy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.PrivacyNotice
import com.mowtiie.mesgard.ui.admin.messageResource
import com.mowtiie.mesgard.ui.common.TitledScreen

private val ContentPadding = 16.dp

/** The privacy notice page of Settings: how data is handled, and the links of each organization. */
@Composable
internal fun PrivacyNoticeScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PrivacyNoticeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TitledScreen(
        title = stringResource(R.string.your_data_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            if (uiState.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            DataSummary()
            uiState.notices.forEach { NoticeLinks(it, showOrganization = uiState.notices.size > 1) }
            uiState.error?.let { ErrorText(it) }
        }
    }
}

/** Shown once after signing in, and again when an organization changes its notice, before the person goes on. */
@Composable
internal fun PrivacyNoticeGateScreen(
    notice: PrivacyNotice,
    isAcknowledging: Boolean,
    error: AdminError?,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState())) {
            Text(
                text = stringResource(R.string.privacy_gate_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = ContentPadding, top = ContentPadding, end = ContentPadding)
                    .semantics { heading() },
            )
            Text(
                text = stringResource(R.string.privacy_gate_text, notice.orgName),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = ContentPadding, top = ContentPadding, end = ContentPadding),
            )
            NoticeLinks(notice, showOrganization = false)
            DataSummary()
            error?.let { ErrorText(it) }
            Button(
                onClick = onContinue,
                enabled = !isAcknowledging,
                modifier = Modifier.fillMaxWidth().padding(ContentPadding),
            ) {
                Text(stringResource(R.string.privacy_gate_continue))
            }
        }
    }
}

@Composable
private fun ErrorText(error: AdminError) {
    Text(
        text = stringResource(error.messageResource()),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(ContentPadding),
    )
}
