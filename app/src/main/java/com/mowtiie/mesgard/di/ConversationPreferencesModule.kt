package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.OfflineFirstConversationPreferencesRepository
import com.mowtiie.mesgard.data.remote.ConversationPreferencesRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseConversationPreferencesRemoteSource
import com.mowtiie.mesgard.domain.repository.ConversationPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ConversationPreferencesModule {
    @Binds
    @Singleton
    fun bindRepository(repository: OfflineFirstConversationPreferencesRepository): ConversationPreferencesRepository

    @Binds
    @Singleton
    fun bindRemoteSource(source: SupabaseConversationPreferencesRemoteSource): ConversationPreferencesRemoteSource
}
