package com.mowtiie.mesgard.data.server

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mowtiie.mesgard.domain.server.ServerConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SharedPreferencesServerConfigStoreTest {
    private val preferences = ApplicationProvider.getApplicationContext<Application>()
        .getSharedPreferences("test_server_config", Context.MODE_PRIVATE)
    private val builtIn = ServerConfig("http://10.0.2.2:54321", "built-in-key")

    @Test
    fun `has no server when nothing is stored and none is built in`() {
        assertNull(SharedPreferencesServerConfigStore(preferences, default = null).current)
    }

    @Test
    fun `uses the built in server until one is saved`() {
        val store = SharedPreferencesServerConfigStore(preferences, builtIn)
        assertEquals(builtIn, store.current)

        store.save(ServerConfig("https://chat.school.example", "school-key"))

        assertEquals(ServerConfig("https://chat.school.example", "school-key"), store.current)
    }

    @Test
    fun `a saved server survives a new store instance`() {
        SharedPreferencesServerConfigStore(preferences, null).save(ServerConfig("https://a.example", "k"))

        val reopened = SharedPreferencesServerConfigStore(preferences, null)

        assertEquals(ServerConfig("https://a.example", "k"), reopened.current)
    }
}
