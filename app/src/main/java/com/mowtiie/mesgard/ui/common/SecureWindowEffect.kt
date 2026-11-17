package com.mowtiie.mesgard.ui.common

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

/**
 * Blocks screenshots and screen recording while the screen shows, whatever the screen privacy setting says. The flag is
 * left alone afterwards when the setting had already set it.
 */
@Composable
internal fun SecureWindowEffect() {
    val window = LocalActivity.current?.window
    DisposableEffect(window) {
        val wasSecure = window != null && window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            if (!wasSecure) window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
