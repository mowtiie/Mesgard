package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.avatar.OfflineFirstAvatarSource
import com.mowtiie.mesgard.data.remote.AvatarRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseAvatarRemoteSource
import com.mowtiie.mesgard.domain.avatar.AvatarSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AvatarModule {
    @Binds
    @Singleton
    fun bindAvatarRemoteSource(source: SupabaseAvatarRemoteSource): AvatarRemoteSource

    @Binds
    @Singleton
    fun bindAvatarSource(source: OfflineFirstAvatarSource): AvatarSource
}
