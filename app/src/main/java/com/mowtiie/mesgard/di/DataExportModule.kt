package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.RemoteDataExportRepository
import com.mowtiie.mesgard.data.remote.DataExportRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseDataExportRemoteSource
import com.mowtiie.mesgard.domain.repository.DataExportRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface DataExportModule {
    @Binds
    @Singleton
    fun bindDataExportRemoteSource(source: SupabaseDataExportRemoteSource): DataExportRemoteSource

    @Binds
    @Singleton
    fun bindDataExportRepository(repository: RemoteDataExportRepository): DataExportRepository
}
