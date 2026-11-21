package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.RemoteLegalHoldRepository
import com.mowtiie.mesgard.data.remote.LegalHoldRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseLegalHoldRemoteSource
import com.mowtiie.mesgard.domain.repository.LegalHoldRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface LegalHoldModule {
    @Binds
    @Singleton
    fun bindLegalHoldRemoteSource(source: SupabaseLegalHoldRemoteSource): LegalHoldRemoteSource

    @Binds
    @Singleton
    fun bindLegalHoldRepository(repository: RemoteLegalHoldRepository): LegalHoldRepository
}
