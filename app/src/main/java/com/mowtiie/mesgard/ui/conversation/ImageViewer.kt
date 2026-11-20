package com.mowtiie.mesgard.ui.conversation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
import com.mowtiie.mesgard.domain.model.Message

private const val BAR_ALPHA = 0.7f
private const val SUBTITLE_ALPHA = 0.85f
private const val BUTTON_CONTAINER_ALPHA = 0.16f
private const val OUTLINE_ALPHA = 0.6f
private val BarPadding = 8.dp
private val FadeHeight = 24.dp

/** The viewer for the message the person opened, or nothing when none of its photos is on the device. */
@Composable
internal fun ConversationImageViewer(
    messages: List<Message>,
    openedId: String,
    onDismiss: () -> Unit,
    onOpenWithAnotherApp: (Attachment) -> Unit,
) {
    val message = messages.firstOrNull { candidate -> candidate.attachments.any { it.id == openedId } } ?: return
    val viewed = viewerPhotosOf(message, openedId) ?: return
    ImageViewer(
        photos = viewed.photos,
        startIndex = viewed.startIndex,
        title = viewerTitle(message, viewed.photos[viewed.startIndex].fileName),
        subtitle = viewerSubtitle(message.createdAtEpochMillis),
        onDismiss = onDismiss,
        onOpenWithAnotherApp = onOpenWithAnotherApp,
    )
}

/**
 * Shows the photos of a message at full size. A swipe turns to the next photo, a tap hides or shows the bar and the
 * system bars, and a photo has pinch zoom, pan and double tap zoom. Swiping is off while a photo is zoomed, so the
 * drag moves the photo.
 */
@Composable
private fun ImageViewer(
    photos: List<Attachment>,
    startIndex: Int,
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    onOpenWithAnotherApp: (Attachment) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = startIndex) { photos.size }
    var isBarShown by remember { mutableStateOf(true) }
    var isZoomed by remember { mutableStateOf(false) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        ViewerSystemBars(isBarShown)
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(state = pagerState, userScrollEnabled = !isZoomed, modifier = Modifier.fillMaxSize()) {
                ZoomableImage(
                    attachment = photos[it],
                    onTap = { isBarShown = !isBarShown },
                    onZoomChanged = { zoomed -> isZoomed = zoomed },
                )
            }
            AnimatedVisibility(visible = isBarShown, enter = fadeIn(), exit = fadeOut()) {
                val current = photos[pagerState.currentPage]
                val position = stringResource(R.string.viewer_position, pagerState.currentPage + 1, photos.size)
                ViewerBar(
                    title = title,
                    subtitle = if (photos.size > 1) "$subtitle, $position" else subtitle,
                    onClose = onDismiss,
                    onOpenWithAnotherApp = { onOpenWithAnotherApp(current) },
                )
            }
        }
    }
}

@Composable
private fun ZoomableImage(attachment: Attachment, onTap: () -> Unit, onZoomChanged: (Boolean) -> Unit) {
    val zoom = remember { ZoomState() }
    val description = stringResource(R.string.attachment_photo, attachment.fileName)
    val context = LocalContext.current
    val request = remember(attachment.localPath, attachment.mimeType) {
        animatedImageRequest(context, attachment, attachment.localPath.orEmpty())
    }
    LaunchedEffect(zoom.isZoomed) { onZoomChanged(zoom.isZoomed) }

    AsyncImage(
        model = request,
        contentDescription = description,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize()
            .onSizeChanged {
                zoom.onLaidOut(Size(it.width.toFloat(), it.height.toFloat()), attachment.width, attachment.height)
            }
            .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }, onDoubleTap = zoom::toggleAt) }
            .pointerInput(Unit) { detectZoomGestures(zoom) }
            .graphicsLayer(
                scaleX = zoom.zoom,
                scaleY = zoom.zoom,
                translationX = zoom.offset.x,
                translationY = zoom.offset.y,
            ),
    )
}

/**
 * A pinch always zooms, and a drag moves the photo only while it is zoomed. A drag of a photo at normal size is left
 * unconsumed, so the pager can turn to the next photo.
 */
private suspend fun PointerInputScope.detectZoomGestures(zoom: ZoomState) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            if (event.changes.size > 1 || zoom.isZoomed) {
                zoom.transform(event.calculateCentroid(), event.calculatePan(), event.calculateZoom())
                event.changes.forEach { if (it.positionChanged()) it.consume() }
            }
        } while (event.changes.any { it.pressed })
    }
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
