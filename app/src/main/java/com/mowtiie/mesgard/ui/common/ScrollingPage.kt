package com.mowtiie.mesgard.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Wider than this, a row puts its switch too far from its label to read as one. */
internal val ReadableWidth = 600.dp

/** A scrolling column that stays at a readable width and is centered on tablets and in landscape. */
@Composable
internal fun ScrollingPage(padding: PaddingValues, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.widthIn(max = ReadableWidth).fillMaxSize().verticalScroll(rememberScrollState()),
            content = content,
        )
    }
}
