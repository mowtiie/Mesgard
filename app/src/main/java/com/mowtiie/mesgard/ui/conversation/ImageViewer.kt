package com.mowtiie.mesgard.ui.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.Attachment
import java.io.File

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f
private const val HALF = 2f

/** Shows a photo that is already on the device at full size, with pinch zoom, pan and double tap zoom. */
@Composable
internal fun ImageViewer(attachment: Attachment, onDismiss: () -> Unit, onOpenWithAnotherApp: () -> Unit) {
    val localPath = attachment.localPath ?: return
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            ZoomableImage(localPath, attachment.fileName)
            ViewerBar(attachment.fileName, onDismiss, onOpenWithAnotherApp)
        }
    }
}

@Composable
private fun ZoomableImage(localPath: String, fileName: String) {
    var zoom by remember { mutableFloatStateOf(MIN_ZOOM) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val description = stringResource(R.string.attachment_photo, fileName)

    fun limit(candidate: Offset, scale: Float): Offset {
        val maxX = size.width * (scale - 1f) / HALF
        val maxY = size.height * (scale - 1f) / HALF
        return Offset(candidate.x.coerceIn(-maxX, maxX), candidate.y.coerceIn(-maxY, maxY))
    }

    AsyncImage(
        model = File(localPath),
        contentDescription = description,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        zoom = if (zoom > MIN_ZOOM) MIN_ZOOM else DOUBLE_TAP_ZOOM
                        offset = Offset.Zero
                    },
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    zoom = (zoom * gestureZoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
                    offset = limit(offset + pan * zoom, zoom)
                }
            }
            .graphicsLayer(scaleX = zoom, scaleY = zoom, translationX = offset.x, translationY = offset.y),
    )
}

@Composable
private fun ViewerBar(fileName: String, onClose: () -> Unit, onOpenWithAnotherApp: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.close), tint = Color.White)
        }
        Text(
            text = fileName,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onOpenWithAnotherApp) {
            Text(stringResource(R.string.attachment_open_with), color = Color.White)
        }
    }
}
