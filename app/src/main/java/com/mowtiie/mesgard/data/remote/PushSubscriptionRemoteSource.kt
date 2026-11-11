package com.mowtiie.mesgard.data.remote

import com.mowtiie.mesgard.domain.push.PushEndpoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val SUBSCRIPTIONS_TABLE = "push_subscriptions"
private const val REGISTER_FUNCTION = "register_push_subscription"

internal interface PushSubscriptionRemoteSource {
    suspend fun register(endpoint: PushEndpoint)

    suspend fun unregister(endpointUrl: String)
}

internal class SupabasePushSubscriptionRemoteSource @Inject constructor(private val client: SupabaseClient) :
    PushSubscriptionRemoteSource {
    override suspend fun register(endpoint: PushEndpoint) {
        client.postgrest.rpc(
            REGISTER_FUNCTION,
            buildJsonObject {
                put("new_endpoint", endpoint.url)
                put("new_p256dh", endpoint.publicKey)
                put("new_auth_secret", endpoint.authSecret)
            },
        )
    }

    override suspend fun unregister(endpointUrl: String) {
        client.from(SUBSCRIPTIONS_TABLE).delete { filter { eq("endpoint", endpointUrl) } }
    }
}
