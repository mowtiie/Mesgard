package com.mowtiie.mesgard.data.push

import android.util.Log
import com.google.firebase.FirebaseException
import com.mowtiie.mesgard.data.remote.DeviceTokenRemoteSource
import com.mowtiie.mesgard.data.remote.RemoteOutcome
import com.mowtiie.mesgard.data.remote.attemptRemote
import com.mowtiie.mesgard.data.remote.toRefreshResult
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.push.PushRegistration
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

private const val TAG = "PushRegistration"

internal class DevicePushRegistration @Inject constructor(
    private val tokenProvider: PushTokenProvider,
    private val remoteSource: DeviceTokenRemoteSource,
) : PushRegistration {
    override suspend fun register(): RefreshResult = withToken { token -> remoteSource.register(token) }

    override suspend fun unregister(): RefreshResult = withToken { token -> remoteSource.unregister(token) }

    private suspend fun withToken(action: suspend (String) -> Unit): RefreshResult {
        val token = fetchToken()
        return when (token) {
            is RemoteOutcome.Failure -> RefreshResult.Failure(token.error)
            is RemoteOutcome.Success -> token.value?.let { attemptRemote(TAG) { action(it) }.toRefreshResult() }
                ?: RefreshResult.Success
        }
    }

    private suspend fun fetchToken(): RemoteOutcome<String?> = try {
        RemoteOutcome.Success(tokenProvider.currentToken())
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: IOException) {
        Log.i(TAG, "The FCM token could not be read", exception)
        RemoteOutcome.Failure(RefreshError.NETWORK_UNAVAILABLE)
    } catch (exception: FirebaseException) {
        Log.w(TAG, "The FCM token could not be read", exception)
        RemoteOutcome.Failure(RefreshError.UNKNOWN)
    } catch (exception: IllegalStateException) {
        Log.w(TAG, "The FCM token could not be read", exception)
        RemoteOutcome.Failure(RefreshError.UNKNOWN)
    }
}
