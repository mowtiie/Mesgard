package com.mowtiie.mesgard.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private const val SLIDE_DURATION_MS = 300
private const val FADE_OUT_DURATION_MS = 90
private const val FADE_IN_DURATION_MS = 210
private val SlideDistance = 30.dp

/**
 * The shared axis pattern on the x axis from Material motion: moving forward slides the old screen out to the start and
 * the new one in from the end, going back reverses it, and both fade. Navigation seeks the back transitions with the
 * predictive back gesture.
 */
internal class SharedAxisTransitions(private val slideDistancePx: Int) {
    val forwardEnter: EnterTransition = enter(fromEnd = true)
    val forwardExit: ExitTransition = exit(towardStart = true)
    val backEnter: EnterTransition = enter(fromEnd = false)
    val backExit: ExitTransition = exit(towardStart = false)

    private fun enter(fromEnd: Boolean): EnterTransition {
        val direction = if (fromEnd) 1 else -1
        return slideInHorizontally(tween(SLIDE_DURATION_MS, easing = FastOutSlowInEasing)) {
            direction * slideDistancePx
        } +
            fadeIn(tween(FADE_IN_DURATION_MS, delayMillis = FADE_OUT_DURATION_MS, easing = FastOutSlowInEasing))
    }

    private fun exit(towardStart: Boolean): ExitTransition {
        val direction = if (towardStart) -1 else 1
        return slideOutHorizontally(tween(SLIDE_DURATION_MS, easing = FastOutSlowInEasing)) {
            direction * slideDistancePx
        } +
            fadeOut(tween(FADE_OUT_DURATION_MS, easing = FastOutSlowInEasing))
    }
}

@Composable
internal fun rememberSharedAxisTransitions(): SharedAxisTransitions {
    val slideDistancePx = with(LocalDensity.current) { SlideDistance.roundToPx() }
    return remember(slideDistancePx) { SharedAxisTransitions(slideDistancePx) }
}
