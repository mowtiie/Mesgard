package com.mowtiie.mesgard.domain.push

import com.mowtiie.mesgard.domain.model.RefreshResult
import kotlinx.coroutines.flow.StateFlow

internal data class PushEndpoint(val url: String, val publicKey: String, val authSecret: String)

internal interface PushRegistration {
    /** The name of the distributor app the user chose, or null while push is not set up. */
    val distributorName: StateFlow<String?>

    /** After sign in: registers with the chosen distributor and sends the stored endpoint to the server. */
    suspend fun start(): RefreshResult

    /** Before sign out: removes the subscription from the server and unregisters from the distributor. */
    suspend fun stop(): RefreshResult

    /** The distributor issued an endpoint for this device. */
    suspend fun saveEndpoint(endpoint: PushEndpoint): RefreshResult

    /** The distributor removed the registration, so the endpoint is no longer valid. */
    suspend fun forgetEndpoint(): RefreshResult

    /** The user picked a distributor app. */
    fun onDistributorChosen()
}
