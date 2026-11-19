package com.mowtiie.mesgard.ui.about

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.common.TitledScreen

private val ContentPadding = 16.dp

@Composable
internal fun OpenSourceLicensesScreenRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    TitledScreen(
        title = stringResource(R.string.licenses_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            Text(
                text = stringResource(R.string.licenses_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(ContentPadding),
            )
            ThirdPartyNotices.forEach { notice ->
                ListItem(
                    headlineContent = { Text(notice.name) },
                    supportingContent = { Text(notice.license) },
                )
            }
        }
    }
}
