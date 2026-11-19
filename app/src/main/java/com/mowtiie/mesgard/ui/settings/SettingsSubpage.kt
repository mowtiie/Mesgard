package com.mowtiie.mesgard.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.common.TitledScreen

/** A full screen page of Settings: a title, a back arrow, a scrolling column and the error snackbar. */
@Composable
internal fun SettingsSubpage(
    @StringRes title: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    error: SettingsError? = null,
    onErrorShown: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = error?.let { stringResource(it.messageResource()) }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onErrorShown()
        }
    }

    TitledScreen(
        title = stringResource(title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()),
            content = content,
        )
    }
}

@StringRes
private fun SettingsError.messageResource(): Int = when (this) {
    SettingsError.DEVICE_LOCK_REQUIRED -> R.string.settings_error_device_lock_required
    SettingsError.PUSH_DISTRIBUTOR_UNAVAILABLE -> R.string.settings_error_push_distributor_unavailable
    SettingsError.PREVIEW_NETWORK_UNAVAILABLE -> R.string.settings_error_preview_network_unavailable
    SettingsError.PREVIEW_UNKNOWN -> R.string.settings_error_preview_unknown
}
