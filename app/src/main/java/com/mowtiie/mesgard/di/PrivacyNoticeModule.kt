package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.RemotePrivacyNoticeRepository
import com.mowtiie.mesgard.data.remote.PrivacyNoticeRemoteSource
import com.mowtiie.mesgard.data.remote.SupabasePrivacyNoticeRemoteSource
import com.mowtiie.mesgard.domain.repository.PrivacyNoticeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface PrivacyNoticeModule {
    @Binds
    @Singleton
    fun bindPrivacyNoticeRemoteSource(source: SupabasePrivacyNoticeRemoteSource): PrivacyNoticeRemoteSource

    @Binds
    @Singleton
    fun bindPrivacyNoticeRepository(repository: RemotePrivacyNoticeRepository): PrivacyNoticeRepository
}
