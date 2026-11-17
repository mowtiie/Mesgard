package com.mowtiie.mesgard.ui.auth

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

private val ProgressSize = 20.dp
private val ProgressStrokeWidth = 2.dp

/** The single high emphasis action of a form. The label gives way to a progress indicator while it works. */
@Composable
internal fun SubmitButton(
    @StringRes label: Int,
    isBusy: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth()) {
        if (isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.size(ProgressSize),
                color = LocalContentColor.current,
                trackColor = Color.Transparent,
                strokeWidth = ProgressStrokeWidth,
            )
        } else {
            Text(stringResource(label))
        }
    }
}
