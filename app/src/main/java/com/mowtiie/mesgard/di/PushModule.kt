package com.mowtiie.mesgard.di

import android.content.Context
import com.mowtiie.mesgard.data.push.DevicePushRegistration
import com.mowtiie.mesgard.data.push.PUSH_ENDPOINT_FILE
import com.mowtiie.mesgard.data.push.PushClient
import com.mowtiie.mesgard.data.push.PushEndpointStore
import com.mowtiie.mesgard.data.push.SharedPreferencesPushEndpointStore
import com.mowtiie.mesgard.data.push.UnifiedPushClient
import com.mowtiie.mesgard.data.remote.PushSubscriptionRemoteSource
import com.mowtiie.mesgard.data.remote.SupabasePushSubscriptionRemoteSource
import com.mowtiie.mesgard.domain.push.PushRegistration
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PushModule {
    @Binds
    abstract fun bindPushClient(client: UnifiedPushClient): PushClient

    @Binds
    abstract fun bindPushRegistration(registration: DevicePushRegistration): PushRegistration

    @Binds
    abstract fun bindPushSubscriptionRemoteSource(
        source: SupabasePushSubscriptionRemoteSource,
    ): PushSubscriptionRemoteSource

    companion object {
        @Provides
        @Singleton
        fun providePushEndpointStore(@ApplicationContext context: Context): PushEndpointStore =
            SharedPreferencesPushEndpointStore(context.getSharedPreferences(PUSH_ENDPOINT_FILE, Context.MODE_PRIVATE))
    }
}
