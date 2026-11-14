package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.RemoteAuditLogRepository
import com.mowtiie.mesgard.data.remote.AuditLogRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseAuditLogRemoteSource
import com.mowtiie.mesgard.domain.repository.AuditLogRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AuditLogModule {
    @Binds
    @Singleton
    fun bindAuditLogRemoteSource(source: SupabaseAuditLogRemoteSource): AuditLogRemoteSource

    @Binds
    @Singleton
    fun bindAuditLogRepository(repository: RemoteAuditLogRepository): AuditLogRepository
}
