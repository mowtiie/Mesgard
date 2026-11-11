package com.mowtiie.mesgard

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mowtiie.mesgard.data.push.EXTRA_CONVERSATION_ID
import com.mowtiie.mesgard.domain.lifecycle.AppVisibility
import com.mowtiie.mesgard.domain.lock.AppLock
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
    internal lateinit var appVisibility: AppVisibility

    private var conversationToOpen by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyScreenPrivacy(deviceSettings.settings.value.screenPrivacyEnabled)
        conversationToOpen = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        enableEdgeToEdge()
        setContent {
            MesgardTheme {
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
