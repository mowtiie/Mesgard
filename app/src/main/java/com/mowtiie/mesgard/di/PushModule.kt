package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.push.DevicePushRegistration
import com.mowtiie.mesgard.data.push.FirebasePushTokenProvider
import com.mowtiie.mesgard.data.push.PushTokenProvider
import com.mowtiie.mesgard.domain.push.PushRegistration
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PushModule {
    @Binds
    abstract fun bindPushTokenProvider(provider: FirebasePushTokenProvider): PushTokenProvider

    @Binds
    abstract fun bindPushRegistration(registration: DevicePushRegistration): PushRegistration
}
