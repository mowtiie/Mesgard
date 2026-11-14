package com.mowtiie.mesgard.di

import android.content.Context
import com.mowtiie.mesgard.BuildConfig
import com.mowtiie.mesgard.data.server.HttpServerChecker
import com.mowtiie.mesgard.data.server.ProcessAppRestarter
import com.mowtiie.mesgard.data.server.SERVER_CONFIG_FILE
import com.mowtiie.mesgard.data.server.SharedPreferencesServerConfigStore
import com.mowtiie.mesgard.domain.server.AppRestarter
import com.mowtiie.mesgard.domain.server.ServerChecker
import com.mowtiie.mesgard.domain.server.ServerConfig
import com.mowtiie.mesgard.domain.server.ServerConfigStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ServerBindingModule {
    @Binds
    fun bindServerChecker(checker: HttpServerChecker): ServerChecker

    @Binds
    fun bindAppRestarter(restarter: ProcessAppRestarter): AppRestarter
}

@Module
@InstallIn(SingletonComponent::class)
internal object ServerModule {
    /** A developer build can carry a server from local.properties; release builds ask on first launch. */
    @Provides
    @Singleton
    fun provideServerConfigStore(@ApplicationContext context: Context): ServerConfigStore {
        val builtIn = ServerConfig(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY)
            .takeIf { it.url.isNotBlank() && it.anonKey.isNotBlank() }
        return SharedPreferencesServerConfigStore(
            context.getSharedPreferences(SERVER_CONFIG_FILE, Context.MODE_PRIVATE),
            builtIn,
        )
    }
}
