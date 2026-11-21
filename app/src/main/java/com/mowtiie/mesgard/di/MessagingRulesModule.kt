package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.RemoteMessagingRulesRepository
import com.mowtiie.mesgard.data.remote.MessagingRulesRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseMessagingRulesRemoteSource
import com.mowtiie.mesgard.domain.repository.MessagingRulesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface MessagingRulesModule {
    @Binds
    @Singleton
    fun bindMessagingRulesRemoteSource(source: SupabaseMessagingRulesRemoteSource): MessagingRulesRemoteSource

    @Binds
    @Singleton
    fun bindMessagingRulesRepository(repository: RemoteMessagingRulesRepository): MessagingRulesRepository
}
