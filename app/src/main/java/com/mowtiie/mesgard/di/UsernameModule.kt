package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.RemoteProfileNoticeRepository
import com.mowtiie.mesgard.data.RemoteUsernameRepository
import com.mowtiie.mesgard.data.remote.ProfileNoticeRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseProfileNoticeRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseUsernameRemoteSource
import com.mowtiie.mesgard.data.remote.UsernameRemoteSource
import com.mowtiie.mesgard.domain.repository.ProfileNoticeRepository
import com.mowtiie.mesgard.domain.repository.UsernameRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface UsernameModule {
    @Binds
    @Singleton
    fun bindUsernameRemoteSource(source: SupabaseUsernameRemoteSource): UsernameRemoteSource

    @Binds
    @Singleton
    fun bindUsernameRepository(repository: RemoteUsernameRepository): UsernameRepository

    @Binds
    @Singleton
    fun bindProfileNoticeRemoteSource(source: SupabaseProfileNoticeRemoteSource): ProfileNoticeRemoteSource

    @Binds
    @Singleton
    fun bindProfileNoticeRepository(repository: RemoteProfileNoticeRepository): ProfileNoticeRepository
}
