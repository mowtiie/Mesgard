package com.mowtiie.mesgard.ui.conversation

import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.decode.Decoder
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.request.ImageRequest
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.data.attachment.StoredFile
import com.mowtiie.mesgard.domain.model.Attachment

private const val BADGE_ALPHA = 0.6f
private val BadgeCorner = 4.dp
private val BadgePaddingHorizontal = 6.dp
private val BadgePaddingVertical = 2.dp

/**
 * A GIF plays only in the viewer. In a bubble it is the still first frame, which the default decoder gives, so a
 * conversation with several GIFs does not animate them all at once.
 */
internal fun animatedImageRequest(context: Context, attachment: Attachment, path: String): ImageRequest =
    ImageRequest.Builder(context)
        .data(StoredFile(path))
        .apply { if (attachment.isGif) decoderFactory(gifDecoderFactory()) }
        .build()

/** The platform decoder from Android 9, which is the faster one, and the library decoder before that. */
private fun gifDecoderFactory(): Decoder.Factory =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) AnimatedImageDecoder.Factory() else GifDecoder.Factory()

/** Tells a still GIF in a bubble from a photo; the viewer plays it. */
@Composable
internal fun GifBadge(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.attachment_gif_badge),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = modifier
            .background(Color.Black.copy(alpha = BADGE_ALPHA), RoundedCornerShape(BadgeCorner))
            .padding(horizontal = BadgePaddingHorizontal, vertical = BadgePaddingVertical),
    )
}
