package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.RemoteReportRepository
import com.mowtiie.mesgard.data.RemoteReportReviewRepository
import com.mowtiie.mesgard.data.remote.ReportRemoteSource
import com.mowtiie.mesgard.data.remote.ReportReviewRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseReportRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseReportReviewRemoteSource
import com.mowtiie.mesgard.domain.repository.ReportRepository
import com.mowtiie.mesgard.domain.repository.ReportReviewRepository
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

    @Binds
    @Singleton
    fun bindReportReviewRemoteSource(source: SupabaseReportReviewRemoteSource): ReportReviewRemoteSource

    @Binds
    @Singleton
    fun bindReportReviewRepository(repository: RemoteReportReviewRepository): ReportReviewRepository
}
