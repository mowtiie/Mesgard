package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.RemoteConversationSearchRepository
import com.mowtiie.mesgard.data.remote.MessageSearchRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseMessageSearchRemoteSource
import com.mowtiie.mesgard.domain.repository.ConversationSearchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SearchModule {
    @Binds
    @Singleton
    fun bindMessageSearchRemoteSource(source: SupabaseMessageSearchRemoteSource): MessageSearchRemoteSource

    @Binds
    @Singleton
    fun bindConversationSearchRepository(repository: RemoteConversationSearchRepository): ConversationSearchRepository
}
