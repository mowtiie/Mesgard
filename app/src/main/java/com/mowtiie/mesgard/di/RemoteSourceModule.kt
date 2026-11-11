package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.remote.ContactRemoteSource
import com.mowtiie.mesgard.data.remote.ConversationRemoteSource
import com.mowtiie.mesgard.data.remote.DeviceTokenRemoteSource
import com.mowtiie.mesgard.data.remote.GroupRemoteSource
import com.mowtiie.mesgard.data.remote.MessageRealtimeSource
import com.mowtiie.mesgard.data.remote.MessageRemoteSource
import com.mowtiie.mesgard.data.remote.PresenceRemoteSource
import com.mowtiie.mesgard.data.remote.ProfileRemoteSource
import com.mowtiie.mesgard.data.remote.SettingsRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseContactRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseConversationRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseDeviceTokenRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseGroupRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseMessageRealtimeSource
import com.mowtiie.mesgard.data.remote.SupabaseMessageRemoteSource
import com.mowtiie.mesgard.data.remote.SupabasePresenceRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseProfileRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseSettingsRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseTypingRemoteSource
import com.mowtiie.mesgard.data.remote.TypingRemoteSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RemoteSourceModule {
    @Binds
    abstract fun bindConversationRemoteSource(source: SupabaseConversationRemoteSource): ConversationRemoteSource

    @Binds
    abstract fun bindMessageRemoteSource(source: SupabaseMessageRemoteSource): MessageRemoteSource

    @Binds
    abstract fun bindMessageRealtimeSource(source: SupabaseMessageRealtimeSource): MessageRealtimeSource

    @Binds
    abstract fun bindProfileRemoteSource(source: SupabaseProfileRemoteSource): ProfileRemoteSource

    @Binds
    abstract fun bindSettingsRemoteSource(source: SupabaseSettingsRemoteSource): SettingsRemoteSource

    @Binds
    abstract fun bindContactRemoteSource(source: SupabaseContactRemoteSource): ContactRemoteSource

    @Binds
    abstract fun bindGroupRemoteSource(source: SupabaseGroupRemoteSource): GroupRemoteSource

    @Binds
    abstract fun bindTypingRemoteSource(source: SupabaseTypingRemoteSource): TypingRemoteSource

    @Binds
    abstract fun bindPresenceRemoteSource(source: SupabasePresenceRemoteSource): PresenceRemoteSource

    @Binds
    abstract fun bindDeviceTokenRemoteSource(source: SupabaseDeviceTokenRemoteSource): DeviceTokenRemoteSource
}
