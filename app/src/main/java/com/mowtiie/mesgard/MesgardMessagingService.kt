package com.mowtiie.mesgard

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mowtiie.mesgard.data.push.PushMessageHandler
import com.mowtiie.mesgard.domain.push.PushRegistration
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MesgardMessagingService : FirebaseMessagingService() {
    @Inject
    internal lateinit var messageHandler: PushMessageHandler

    @Inject
    internal lateinit var pushRegistration: PushRegistration

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch { pushRegistration.register() }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        messageHandler.handle(message.data)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
