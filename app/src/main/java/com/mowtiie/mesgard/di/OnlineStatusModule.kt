package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.RemoteOnlineStatusRepository
import com.mowtiie.mesgard.data.remote.OnlineStatusRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseOnlineStatusRemoteSource
import com.mowtiie.mesgard.domain.repository.OnlineStatusRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface OnlineStatusModule {
    @Binds
    @Singleton
    fun bindOnlineStatusRemoteSource(source: SupabaseOnlineStatusRemoteSource): OnlineStatusRemoteSource

    @Binds
    @Singleton
    fun bindOnlineStatusRepository(repository: RemoteOnlineStatusRepository): OnlineStatusRepository
}
