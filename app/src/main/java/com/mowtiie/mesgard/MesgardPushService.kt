package com.mowtiie.mesgard

import android.util.Log
import com.mowtiie.mesgard.data.push.PushMessageHandler
import com.mowtiie.mesgard.domain.push.PushEndpoint
import com.mowtiie.mesgard.domain.push.PushRegistration
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.PushService
import org.unifiedpush.android.connector.data.PushEndpoint as DistributorEndpoint
import org.unifiedpush.android.connector.data.PushMessage

private const val TAG = "MesgardPushService"

@AndroidEntryPoint
class MesgardPushService : PushService() {
    @Inject
    internal lateinit var messageHandler: PushMessageHandler

    @Inject
    internal lateinit var pushRegistration: PushRegistration

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessage(message: PushMessage, instance: String) {
        if (!message.decrypted) {
            Log.w(TAG, "Ignored a push message that could not be decrypted")
            return
        }
        messageHandler.handle(message.content)
    }

    override fun onNewEndpoint(endpoint: DistributorEndpoint, instance: String) {
        val keys = endpoint.pubKeySet
        if (keys == null) {
            Log.w(TAG, "Ignored an endpoint without a key set, so messages could not be encrypted")
            return
        }
        scope.launch { pushRegistration.saveEndpoint(PushEndpoint(endpoint.url, keys.pubKey, keys.auth)) }
    }

    override fun onRegistrationFailed(reason: FailedReason, instance: String) {
        Log.w(TAG, "The distributor could not register this device: $reason")
    }

    override fun onUnregistered(instance: String) {
        scope.launch { pushRegistration.forgetEndpoint() }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
