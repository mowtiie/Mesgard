package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.RemoteGroupAccessRepository
import com.mowtiie.mesgard.data.remote.GroupAccessRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseGroupAccessRemoteSource
import com.mowtiie.mesgard.domain.repository.GroupAccessRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface GroupAccessModule {
    @Binds
    @Singleton
    fun bindGroupAccessRemoteSource(source: SupabaseGroupAccessRemoteSource): GroupAccessRemoteSource

    @Binds
    @Singleton
    fun bindGroupAccessRepository(repository: RemoteGroupAccessRepository): GroupAccessRepository
}
