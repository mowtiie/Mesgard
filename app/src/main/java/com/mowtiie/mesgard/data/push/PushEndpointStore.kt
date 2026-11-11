package com.mowtiie.mesgard.data.push

import android.content.SharedPreferences
import com.mowtiie.mesgard.domain.push.PushEndpoint

internal const val PUSH_ENDPOINT_FILE = "push_endpoint"
private const val KEY_URL = "url"
private const val KEY_PUBLIC_KEY = "public_key"
private const val KEY_AUTH_SECRET = "auth_secret"

internal interface PushEndpointStore {
    fun read(): PushEndpoint?

    fun save(endpoint: PushEndpoint)

    fun clear()
}

internal class SharedPreferencesPushEndpointStore(private val preferences: SharedPreferences) : PushEndpointStore {
    override fun read(): PushEndpoint? {
        val url = preferences.getString(KEY_URL, null)
        val publicKey = preferences.getString(KEY_PUBLIC_KEY, null)
        val authSecret = preferences.getString(KEY_AUTH_SECRET, null)
        if (url == null || publicKey == null || authSecret == null) return null
        return PushEndpoint(url, publicKey, authSecret)
    }

    override fun save(endpoint: PushEndpoint) {
        preferences.edit()
            .putString(KEY_URL, endpoint.url)
            .putString(KEY_PUBLIC_KEY, endpoint.publicKey)
            .putString(KEY_AUTH_SECRET, endpoint.authSecret)
            .apply()
    }

    override fun clear() {
        preferences.edit().clear().apply()
    }
}
