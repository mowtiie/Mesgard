package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.RemoteFormerMembersRepository
import com.mowtiie.mesgard.data.remote.FormerMembersRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseFormerMembersRemoteSource
import com.mowtiie.mesgard.domain.repository.FormerMembersRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface FormerMembersModule {
    @Binds
    @Singleton
    fun bindFormerMembersRemoteSource(source: SupabaseFormerMembersRemoteSource): FormerMembersRemoteSource

    @Binds
    @Singleton
    fun bindFormerMembersRepository(repository: RemoteFormerMembersRepository): FormerMembersRepository
}
