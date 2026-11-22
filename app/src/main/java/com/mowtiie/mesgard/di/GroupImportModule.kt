package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.RemoteGroupImportRepository
import com.mowtiie.mesgard.data.remote.GroupImportRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseGroupImportRemoteSource
import com.mowtiie.mesgard.domain.repository.GroupImportRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface GroupImportModule {
    @Binds
    @Singleton
    fun bindGroupImportRemoteSource(source: SupabaseGroupImportRemoteSource): GroupImportRemoteSource

    @Binds
    @Singleton
    fun bindGroupImportRepository(repository: RemoteGroupImportRepository): GroupImportRepository
}
