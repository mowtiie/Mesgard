package com.mowtiie.mesgard.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.mowtiie.mesgard.ui.connectivity.ConnectionBanner
import com.mowtiie.mesgard.ui.connectivity.LocalConnectionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TitledScreen(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopAppBar(title = { Text(title) }, navigationIcon = navigationIcon, scrollBehavior = scrollBehavior)
                ConnectionBanner(status = LocalConnectionStatus.current)
            }
        },
        snackbarHost = snackbarHost,
        content = content,
    )
}
