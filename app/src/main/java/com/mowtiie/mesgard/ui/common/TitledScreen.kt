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
import androidx.compose.ui.text.font.FontFamily
import com.mowtiie.mesgard.ui.connectivity.ConnectionBanner
import com.mowtiie.mesgard.ui.connectivity.LocalConnectionStatus

/**
 * A small top app bar with a serif headline. [actions] are the trailing icon buttons; when [searchBar] is given it
 * replaces the whole app bar, which is how search is shown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TitledScreen(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    searchBar: (@Composable () -> Unit)? = null,
    snackbarHost: @Composable () -> Unit = {},
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
                        title = { Text(text = title, style = TitleStyle()) },
                        navigationIcon = navigationIcon,
                        actions = actions,
                        scrollBehavior = scrollBehavior,
                    )
                }
                ConnectionBanner(status = LocalConnectionStatus.current)
            }
        },
        snackbarHost = snackbarHost,
        content = content,
    )
}

@Composable
private fun TitleStyle() = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif)
