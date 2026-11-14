package com.mowtiie.mesgard.data.server

import android.content.SharedPreferences
import com.mowtiie.mesgard.domain.server.ServerConfig
import com.mowtiie.mesgard.domain.server.ServerConfigStore

internal const val SERVER_CONFIG_FILE = "server_config"
private const val KEY_URL = "url"
private const val KEY_ANON_KEY = "anon_key"

/** A server chosen on the device wins over [default], the one a developer build was compiled with. */
internal class SharedPreferencesServerConfigStore(
    private val preferences: SharedPreferences,
    private val default: ServerConfig?,
) : ServerConfigStore {
    override val current: ServerConfig?
        get() {
            val url = preferences.getString(KEY_URL, null)
            val key = preferences.getString(KEY_ANON_KEY, null)
            return if (url != null && key != null) ServerConfig(url, key) else default
        }

    override fun save(config: ServerConfig) {
        preferences.edit().putString(KEY_URL, config.url).putString(KEY_ANON_KEY, config.anonKey).commit()
    }
}
