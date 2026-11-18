package com.mowtiie.mesgard.ui.conversation

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.SearchHit
import com.mowtiie.mesgard.ui.common.EmptyState

private const val DATE_FLAGS = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH
private const val SNIPPET_LINES = 3

/** What the search finds, newest first. Choosing a hit closes the search and scrolls to that message. */
@Composable
internal fun SearchResults(
    state: ConversationSearchUiState,
    onHitSelected: (SearchHit) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            !state.isLongEnough -> Hint(R.string.conversation_search_hint)
            state.hasFailed -> Hint(R.string.conversation_search_error)
            state.showNoResults ->
                EmptyState(R.drawable.illustration_search, R.string.search_no_results, R.string.search_no_results_hint)
            else -> HitList(state, onHitSelected, onLoadMore)
        }
        if (state.isLoading && state.hits.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun Hint(message: Int) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.TopCenter) {
        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HitList(state: ConversationSearchUiState, onHitSelected: (SearchHit) -> Unit, onLoadMore: () -> Unit) {
    val context = LocalContext.current
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(state.hits, key = { it.messageId }) { hit ->
            val snippet = remember(hit.body, state.query) { searchSnippet(hit.body, state.query) }
            ListItem(
                modifier = Modifier.fillMaxWidth().clickable { onHitSelected(hit) },
                overlineContent = { Text(hit.senderName) },
                headlineContent = { Text(snippet, maxLines = SNIPPET_LINES, overflow = TextOverflow.Ellipsis) },
                supportingContent = { Text(DateUtils.formatDateTime(context, hit.createdAtEpochMillis, DATE_FLAGS)) },
            )
        }
        if (state.hasMore) {
            item(key = "load-more") { LaunchedEffect(Unit) { onLoadMore() } }
        }
    }
}
