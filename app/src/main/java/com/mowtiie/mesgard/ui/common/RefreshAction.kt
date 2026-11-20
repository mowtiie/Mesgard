package com.mowtiie.mesgard.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import com.mowtiie.mesgard.R

/**
 * Offers Refresh to screen readers on an element, for a screen whose refresh is a pull. The title is read as one item
 * with the action. [onRefresh] null leaves the element as it is.
 */
@Composable
internal fun Modifier.refreshAction(onRefresh: (() -> Unit)?): Modifier {
    if (onRefresh == null) return this
    val label = stringResource(R.string.refresh)
    return semantics(mergeDescendants = true) {
        customActions = listOf(
            CustomAccessibilityAction(label) {
                onRefresh()
                true
            },
        )
    }
}
