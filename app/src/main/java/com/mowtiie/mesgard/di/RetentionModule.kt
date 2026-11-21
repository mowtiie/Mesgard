package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.RemoteRetentionRepository
import com.mowtiie.mesgard.data.remote.RetentionRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseRetentionRemoteSource
import com.mowtiie.mesgard.domain.repository.RetentionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface RetentionModule {
    @Binds
    @Singleton
    fun bindRetentionRemoteSource(source: SupabaseRetentionRemoteSource): RetentionRemoteSource

    @Binds
    @Singleton
    fun bindRetentionRepository(repository: RemoteRetentionRepository): RetentionRepository
}
