package com.mowtiie.mesgard.ui.about

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.mowtiie.mesgard.BuildConfig
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.ServerStatus
import com.mowtiie.mesgard.ui.common.TitledScreen
import com.mowtiie.mesgard.ui.server.LocalServerStatus
import kotlinx.coroutines.launch

private const val REPOSITORY_URL = "https://github.com/mowtiie/Mesgard"
private const val LICENSE_URL = "$REPOSITORY_URL/blob/master/LICENSE"
private const val ISSUES_URL = "$REPOSITORY_URL/issues"
private const val SECURITY_POLICY_URL = "$REPOSITORY_URL/security/policy"
private val ContentPadding = 16.dp

@Composable
internal fun AboutScreenRoute(
    onBack: () -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AboutViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val fingerprint = remember { signingFingerprint(context) }
    val linkError = stringResource(R.string.about_error_open_link)
    val copied = stringResource(R.string.message_copied)

    fun openLink(url: String) {
        if (!context.tryOpen(url)) scope.launch { snackbarHostState.showSnackbar(linkError) }
    }

    fun copyFingerprint(text: String) {
        scope.launch {
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(null, text)))
            snackbarHostState.showSnackbar(copied)
        }
    }

    TitledScreen(
        title = stringResource(R.string.about_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            AboutHeader()
            AppSection(viewModel.serverHost, LocalServerStatus.current, fingerprint, ::copyFingerprint)
            ProjectSection(onOpenLink = ::openLink, onOpenLicenses = onOpenLicenses)
            DataSection()
        }
    }
}

@Composable
private fun AboutHeader() {
    Column(modifier = Modifier.padding(ContentPadding)) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Serif,
        )
        Text(
            text = stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AppSection(
    serverHost: String?,
    serverStatus: ServerStatus,
    fingerprint: String?,
    onCopyFingerprint: (String) -> Unit,
) {
    AboutHeading(R.string.about_section_app)
    ListItem(
        headlineContent = { Text(stringResource(R.string.about_version)) },
        supportingContent = {
            Text(stringResource(R.string.about_version_value, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
        },
    )
    ListItem(
        headlineContent = { Text(stringResource(R.string.about_server)) },
        supportingContent = {
            Column {
                Text(serverHost ?: stringResource(R.string.about_server_not_set))
                if (serverStatus == ServerStatus.OUTDATED) {
                    Text(
                        text = stringResource(R.string.about_server_outdated),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
    )
    ListItem(
        modifier = fingerprint?.let {
            Modifier.clickable(
                onClickLabel = stringResource(R.string.message_copy),
                role = Role.Button,
                onClick = { onCopyFingerprint(it) },
            )
        } ?: Modifier,
        headlineContent = { Text(stringResource(R.string.about_signature)) },
        supportingContent = {
            Text(
                text = fingerprint ?: stringResource(R.string.about_signature_unavailable),
                fontFamily = FontFamily.Monospace,
            )
        },
    )
}

@Composable
private fun ProjectSection(onOpenLink: (String) -> Unit, onOpenLicenses: () -> Unit) {
    AboutHeading(R.string.about_section_project)
    LinkRow(R.string.about_license, R.string.about_license_value) { onOpenLink(LICENSE_URL) }
    LinkRow(R.string.about_source, R.string.about_source_value) { onOpenLink(REPOSITORY_URL) }
    LinkRow(R.string.about_issue, R.string.about_issue_summary) { onOpenLink(ISSUES_URL) }
    LinkRow(R.string.about_security, R.string.about_security_summary) { onOpenLink(SECURITY_POLICY_URL) }
    LinkRow(R.string.about_licenses, R.string.about_licenses_summary, onOpenLicenses)
}

@Composable
private fun DataSection() {
    AboutHeading(R.string.about_data_title)
    Text(
        text = stringResource(R.string.about_data_note),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = ContentPadding).padding(bottom = ContentPadding),
    )
}

@Composable
private fun LinkRow(@StringRes title: Int, @StringRes summary: Int, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
        headlineContent = { Text(stringResource(title)) },
        supportingContent = { Text(stringResource(summary)) },
    )
}

@Composable
private fun AboutHeading(@StringRes title: Int) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = ContentPadding, end = ContentPadding, top = ContentPadding)
            .semantics { heading() },
    )
}

/** Opens [url] in the browser. It reports false when no app on the device can open it. */
private fun Context.tryOpen(url: String): Boolean = try {
    startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (_: ActivityNotFoundException) {
    false
}
