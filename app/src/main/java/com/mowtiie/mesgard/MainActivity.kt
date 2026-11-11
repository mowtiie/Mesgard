package com.mowtiie.mesgard

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mowtiie.mesgard.data.push.EXTRA_CONVERSATION_ID
import com.mowtiie.mesgard.domain.lifecycle.AppVisibility
import com.mowtiie.mesgard.domain.lock.AppLock
import com.mowtiie.mesgard.domain.repository.DeviceSettingsRepository
import com.mowtiie.mesgard.ui.MesgardApp
import com.mowtiie.mesgard.ui.theme.MesgardTheme
import com.mowtiie.mesgard.ui.theme.isDark
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

private val LIGHT_SCRIM = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
private val DARK_SCRIM = Color.argb(0x80, 0x1B, 0x1B, 0x1B)

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject
    internal lateinit var deviceSettings: DeviceSettingsRepository

    @Inject
    internal lateinit var appLock: AppLock

    @Inject
    internal lateinit var appVisibility: AppVisibility

    private var conversationToOpen by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyScreenPrivacy(deviceSettings.settings.value.screenPrivacyEnabled)
        conversationToOpen = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        setContent {
            val settings by deviceSettings.settings.collectAsStateWithLifecycle()
            val darkTheme = settings.themeMode.isDark(isSystemInDarkTheme())
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { darkTheme },
                )
                onDispose {}
            }
            MesgardTheme(darkTheme = darkTheme, contrast = settings.contrast, dynamicColor = settings.dynamicColor) {
                MesgardApp(
                    conversationToOpen = conversationToOpen,
                    onConversationOpened = { conversationToOpen = null },
                )
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                deviceSettings.settings.collect { applyScreenPrivacy(it.screenPrivacyEnabled) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_CONVERSATION_ID)?.let { conversationToOpen = it }
    }

    override fun onStart() {
        super.onStart()
        appLock.onAppForegrounded()
        appVisibility.setForeground(true)
    }

    override fun onStop() {
        super.onStop()
        if (isChangingConfigurations) return
        appLock.onAppBackgrounded()
        appVisibility.setForeground(false)
    }

    private fun applyScreenPrivacy(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
