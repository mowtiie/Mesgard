package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.OfflineFirstMentionRepository
import com.mowtiie.mesgard.data.remote.MentionRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseMentionRemoteSource
import com.mowtiie.mesgard.domain.repository.MentionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface MentionModule {
    @Binds
    @Singleton
    fun bindMentionRemoteSource(source: SupabaseMentionRemoteSource): MentionRemoteSource

    @Binds
    @Singleton
    fun bindMentionRepository(repository: OfflineFirstMentionRepository): MentionRepository
}
