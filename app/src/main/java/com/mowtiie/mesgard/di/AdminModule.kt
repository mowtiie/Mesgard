package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.RemoteAdminRepository
import com.mowtiie.mesgard.data.remote.AdminRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseAdminRemoteSource
import com.mowtiie.mesgard.domain.repository.AdminRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AdminModule {
    @Binds
    @Singleton
    fun bindAdminRemoteSource(source: SupabaseAdminRemoteSource): AdminRemoteSource

    @Binds
    @Singleton
    fun bindAdminRepository(repository: RemoteAdminRepository): AdminRepository
}
