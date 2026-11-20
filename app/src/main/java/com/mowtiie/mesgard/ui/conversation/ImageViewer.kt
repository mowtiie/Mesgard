package com.mowtiie.mesgard.ui.conversation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil3.compose.AsyncImage
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.Attachment

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f
private const val HALF = 2f
private const val BAR_ALPHA = 0.7f
private const val SUBTITLE_ALPHA = 0.85f
private const val BUTTON_CONTAINER_ALPHA = 0.16f
private const val OUTLINE_ALPHA = 0.6f
private val BarPadding = 8.dp
private val FadeHeight = 24.dp

/** Shows a photo that is already on the device at full size, with pinch zoom, pan and double tap zoom. A tap hides
 * or shows the bar and the system bars. */
@Composable
internal fun ImageViewer(
    attachment: Attachment,
    title: String,
    subtitle: String?,
    onDismiss: () -> Unit,
    onOpenWithAnotherApp: () -> Unit,
) {
    val localPath = attachment.localPath ?: return
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        var isBarShown by remember { mutableStateOf(true) }
        ViewerSystemBars(isBarShown)
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            ZoomableImage(localPath, attachment, onTap = { isBarShown = !isBarShown })
            AnimatedVisibility(visible = isBarShown, enter = fadeIn(), exit = fadeOut()) {
                ViewerBar(title, subtitle, onDismiss, onOpenWithAnotherApp)
            }
        }
    }
}

@Composable
private fun ZoomableImage(localPath: String, attachment: Attachment, onTap: () -> Unit) {
    var zoom by remember { mutableFloatStateOf(MIN_ZOOM) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val description = stringResource(R.string.attachment_photo, attachment.fileName)
    val container = Size(size.width.toFloat(), size.height.toFloat())
    val center = Offset(container.width / HALF, container.height / HALF)
    val displayed = fittedSize(attachment.width, attachment.height, container)

    val context = LocalContext.current
    val request = remember(localPath, attachment.mimeType) { animatedImageRequest(context, attachment, localPath) }

    AsyncImage(
        model = request,
        contentDescription = description,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { tap ->
                        val newZoom = if (zoom > MIN_ZOOM) MIN_ZOOM else DOUBLE_TAP_ZOOM
                        offset =
                            clampOffset(
                                offsetKeepingFocus(tap, center, zoom, newZoom, offset),
                                newZoom,
                                displayed,
                                container,
                            )
                        zoom = newZoom
                    },
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, gestureZoom, _ ->
                    val newZoom = (zoom * gestureZoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
                    val keptFocus = offsetKeepingFocus(centroid, center, zoom, newZoom, offset)
                    offset = clampOffset(keptFocus + pan, newZoom, displayed, container)
                    zoom = newZoom
                }
            }
            .graphicsLayer(scaleX = zoom, scaleY = zoom, translationX = offset.x, translationY = offset.y),
    )
}

/**
 * The viewer is black in both themes, so the status and navigation bar icons are always light. Otherwise the light
 * theme would draw dark icons on the dark bar. They hide with the bar, and a swipe from the edge brings them back.
 */
@Composable
private fun ViewerSystemBars(isShown: Boolean) {
    val view = LocalView.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (isShown) show(WindowInsetsCompat.Type.systemBars()) else hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}

/**
 * The bar is a dark, mostly opaque band with a soft edge, so white text reads on any photo (about 9 to 1 even on pure
 * white). The buttons have containers, as Material 3 asks for icon buttons that float over an image.
 */
@Composable
private fun ViewerBar(title: String, subtitle: String?, onClose: () -> Unit, onOpenWithAnotherApp: () -> Unit) {
    val band = Color.Black.copy(alpha = BAR_ALPHA)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(band).statusBarsPadding().padding(horizontal = BarPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BarPadding),
        ) {
            FilledTonalIconButton(
                onClick = onClose,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color.White.copy(alpha = BUTTON_CONTAINER_ALPHA),
                    contentColor = Color.White,
                ),
            ) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.close))
            }
            Column(modifier = Modifier.weight(1f).padding(vertical = BarPadding)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = SUBTITLE_ALPHA),
                    )
                }
            }
            OutlinedIconButton(
                onClick = onOpenWithAnotherApp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = OUTLINE_ALPHA)),
                colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = Color.White),
            ) {
                Icon(
                    painterResource(R.drawable.ic_open_in_new),
                    contentDescription = stringResource(R.string.attachment_open_with),
                )
            }
        }
        Spacer(
            modifier = Modifier.fillMaxWidth().height(FadeHeight)
                .background(Brush.verticalGradient(listOf(band, Color.Transparent))),
        )
    }
}
