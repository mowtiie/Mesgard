package com.mowtiie.mesgard.data.push

import com.mowtiie.mesgard.data.remote.PushSubscriptionRemoteSource
import com.mowtiie.mesgard.data.remote.RemoteOutcome
import com.mowtiie.mesgard.data.remote.attemptRemote
import com.mowtiie.mesgard.data.remote.toRefreshResult
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.push.PushEndpoint
import com.mowtiie.mesgard.domain.push.PushRegistration
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "PushRegistration"

@Singleton
internal class DevicePushRegistration @Inject constructor(
    private val client: PushClient,
    private val store: PushEndpointStore,
    private val remoteSource: PushSubscriptionRemoteSource,
) : PushRegistration {
    private val mutableDistributorName = MutableStateFlow(client.distributorName())

    override val distributorName: StateFlow<String?> = mutableDistributorName.asStateFlow()

    override suspend fun start(): RefreshResult {
        mutableDistributorName.value = client.distributorName()
        if (mutableDistributorName.value != null) client.register()
        val endpoint = store.read() ?: return RefreshResult.Success
        return attemptRemote(TAG) { remoteSource.register(endpoint) }.toRefreshResult()
    }

    override suspend fun stop(): RefreshResult {
        val outcome = store.read()?.let { endpoint -> attemptRemote(TAG) { remoteSource.unregister(endpoint.url) } }
        if (mutableDistributorName.value != null) client.unregister()
        store.clear()
        return outcome?.toRefreshResult() ?: RefreshResult.Success
    }

    override suspend fun saveEndpoint(endpoint: PushEndpoint): RefreshResult {
        store.save(endpoint)
        return attemptRemote(TAG) { remoteSource.register(endpoint) }.toRefreshResult()
    }

    override suspend fun forgetEndpoint(): RefreshResult {
        val endpoint = store.read() ?: return RefreshResult.Success
        store.clear()
        return when (val outcome = attemptRemote(TAG) { remoteSource.unregister(endpoint.url) }) {
            is RemoteOutcome.Success -> RefreshResult.Success
            is RemoteOutcome.Failure -> RefreshResult.Failure(outcome.error)
        }
    }

    override fun onDistributorChosen() {
        mutableDistributorName.value = client.distributorName()
        client.register()
    }
}
