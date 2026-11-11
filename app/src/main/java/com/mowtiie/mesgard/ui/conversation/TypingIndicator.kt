package com.mowtiie.mesgard.ui.conversation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R

private const val MAX_NAMED_TYPERS = 2

@Composable
internal fun TypingIndicator(names: List<String>, modifier: Modifier = Modifier) {
    Text(
        text = typingText(names),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun typingText(names: List<String>): String = when {
    names.size > MAX_NAMED_TYPERS -> stringResource(R.string.typing_several)
    names.size == MAX_NAMED_TYPERS -> stringResource(R.string.typing_two, names[0], names[1])
    else -> stringResource(R.string.typing_one, names.first())
}
