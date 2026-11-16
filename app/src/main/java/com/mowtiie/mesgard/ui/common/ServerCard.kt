package com.mowtiie.mesgard.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R

private val CardPadding = 16.dp
private val ButtonPadding = 8.dp
private val TextPadding = 12.dp

/** Which server the person signs in to, with the way to change it. Only the host shows, never the access key. */
@Composable
internal fun ServerCard(host: String, onChange: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val changeLabel = stringResource(R.string.server_setup_change)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(
                start = CardPadding,
                top = ButtonPadding,
                end = ButtonPadding,
                bottom = ButtonPadding,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_dns),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = TextPadding).semantics(mergeDescendants = true) {},
            ) {
                Text(
                    text = stringResource(R.string.server_card_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = host,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(
                onClick = onChange,
                enabled = enabled,
                modifier = Modifier.semantics {
                    contentDescription =
                        changeLabel
                },
            ) {
                Text(stringResource(R.string.server_card_change))
            }
        }
    }
}
