package com.mowtiie.mesgard.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val FormMaxWidth = 400.dp
private val ContentPadding = 24.dp
private val FieldSpacing = 16.dp

/**
 * A form that is centered on the screen. It scrolls when it does not fit, so the keyboard, landscape and large fonts
 * never hide the button. [banner] sits above the form and [snackbarHost] at the bottom.
 */
@Composable
internal fun CenteredForm(
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {},
    banner: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(modifier = modifier.fillMaxSize(), snackbarHost = snackbarHost) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            banner()
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth().imePadding()) {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(min = maxHeight).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Column(
                        modifier = Modifier.widthIn(max = FormMaxWidth).padding(horizontal = ContentPadding),
                        verticalArrangement = Arrangement.spacedBy(FieldSpacing),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        content = content,
                    )
                }
            }
        }
    }
}
