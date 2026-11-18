package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.RemotePinRepository
import com.mowtiie.mesgard.data.remote.PinRemoteSource
import com.mowtiie.mesgard.data.remote.SupabasePinRemoteSource
import com.mowtiie.mesgard.domain.repository.PinRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface PinModule {
    @Binds
    @Singleton
    fun bindPinRemoteSource(source: SupabasePinRemoteSource): PinRemoteSource

    @Binds
    @Singleton
    fun bindPinRepository(repository: RemotePinRepository): PinRepository
}
