package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.RemoteAnnouncementRepository
import com.mowtiie.mesgard.data.remote.AnnouncementRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseAnnouncementRemoteSource
import com.mowtiie.mesgard.domain.repository.AnnouncementRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AnnouncementModule {
    @Binds
    @Singleton
    fun bindAnnouncementRemoteSource(source: SupabaseAnnouncementRemoteSource): AnnouncementRemoteSource

    @Binds
    @Singleton
    fun bindAnnouncementRepository(repository: RemoteAnnouncementRepository): AnnouncementRepository
}
