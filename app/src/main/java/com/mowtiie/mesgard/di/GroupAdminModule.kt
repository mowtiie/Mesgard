package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.RemoteGroupAdminRepository
import com.mowtiie.mesgard.data.remote.GroupAdminRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseGroupAdminRemoteSource
import com.mowtiie.mesgard.domain.repository.GroupAdminRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface GroupAdminModule {
    @Binds
    @Singleton
    fun bindGroupAdminRemoteSource(source: SupabaseGroupAdminRemoteSource): GroupAdminRemoteSource

    @Binds
    @Singleton
    fun bindGroupAdminRepository(repository: RemoteGroupAdminRepository): GroupAdminRepository
}
