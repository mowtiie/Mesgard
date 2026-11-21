package com.mowtiie.mesgard.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

private val IllustrationMaxHeight = 160.dp
private val IllustrationPadding = 16.dp

/**
 * An illustration at the top of a page that is critical or that a person meets once. It only decorates, so screen
 * readers skip it and the text beside it carries the meaning.
 */
@Composable
internal fun PageIllustration(@DrawableRes illustration: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(top = IllustrationPadding), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(illustration),
            contentDescription = null,
            modifier = Modifier.heightIn(max = IllustrationMaxHeight),
            contentScale = ContentScale.Fit,
        )
    }
}
