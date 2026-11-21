package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.RemoteReportRepository
import com.mowtiie.mesgard.data.remote.ReportRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseReportRemoteSource
import com.mowtiie.mesgard.domain.repository.ReportRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ReportModule {
    @Binds
    @Singleton
    fun bindReportRemoteSource(source: SupabaseReportRemoteSource): ReportRemoteSource

    @Binds
    @Singleton
    fun bindReportRepository(repository: RemoteReportRepository): ReportRepository
}
