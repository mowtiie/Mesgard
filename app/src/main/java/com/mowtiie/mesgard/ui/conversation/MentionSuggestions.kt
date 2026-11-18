package com.mowtiie.mesgard.ui.conversation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.MentionCandidate
import com.mowtiie.mesgard.ui.common.InitialAvatar

/** The people that match what follows the @, above the composer. Nothing shows when nobody matches. */
@Composable
internal fun MentionSuggestions(
    suggestions: List<MentionCandidate>,
    onSelected: (MentionCandidate) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (suggestions.isEmpty()) return
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column {
            suggestions.forEach { candidate ->
                ListItem(
                    modifier = Modifier.clickable(
                        onClickLabel = stringResource(R.string.mention_choose, candidate.displayName),
                    ) { onSelected(candidate) },
                    leadingContent = { InitialAvatar(candidate.displayName) },
                    headlineContent = { Text(candidate.displayName) },
                )
            }
        }
    }
}
