package com.mowtiie.mesgard.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import com.mowtiie.mesgard.ui.connectivity.ConnectionSnackbarHost

/**
 * A small top app bar with a serif headline. [actions] are the trailing icon buttons; when [searchBar] is given it
 * replaces the whole app bar, which is how search is shown. [onRefresh], when the screen has a pull to refresh, is
 * offered to screen readers as a Refresh action of the title, because a pull is a gesture they cannot make. It is null
 * while a refresh is already running.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TitledScreen(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    searchBar: (@Composable () -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                if (searchBar != null) {
                    searchBar()
                } else {
                    TopAppBar(
                        title = { TitleWithSubtitle(title, subtitle, onRefresh) },
                        navigationIcon = navigationIcon,
                        actions = actions,
                        scrollBehavior = scrollBehavior,
                    )
                }
            }
        },
        snackbarHost = {
            Column {
                snackbarHost()
                ConnectionSnackbarHost()
            }
        },
        floatingActionButton = floatingActionButton,
        content = content,
    )
}

@Composable
private fun TitleWithSubtitle(title: String, subtitle: String?, onRefresh: (() -> Unit)?) {
    Column(modifier = Modifier.refreshAction(onRefresh)) {
        Text(text = title, style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif))
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}
