package com.mowtiie.mesgard

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mowtiie.mesgard.domain.lock.AppLock
import com.mowtiie.mesgard.domain.presence.PresenceTracker
import com.mowtiie.mesgard.domain.repository.DeviceSettingsRepository
import com.mowtiie.mesgard.ui.MesgardApp
import com.mowtiie.mesgard.ui.theme.MesgardTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject
    internal lateinit var deviceSettings: DeviceSettingsRepository

    @Inject
    internal lateinit var appLock: AppLock

    @Inject
    internal lateinit var presenceTracker: PresenceTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyScreenPrivacy(deviceSettings.settings.value.screenPrivacyEnabled)
        enableEdgeToEdge()
        setContent {
            MesgardTheme {
                MesgardApp()
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                deviceSettings.settings.collect { applyScreenPrivacy(it.screenPrivacyEnabled) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appLock.onAppForegrounded()
        presenceTracker.onAppForegrounded()
    }

    override fun onStop() {
        super.onStop()
        if (isChangingConfigurations) return
        appLock.onAppBackgrounded()
        presenceTracker.onAppBackgrounded()
    }

    private fun applyScreenPrivacy(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
