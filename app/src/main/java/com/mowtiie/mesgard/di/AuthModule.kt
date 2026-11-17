package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.auth.SupabaseMfaRepository
import com.mowtiie.mesgard.data.remote.MfaRemoteSource
import com.mowtiie.mesgard.data.remote.PasswordRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseMfaRemoteSource
import com.mowtiie.mesgard.data.remote.SupabasePasswordRemoteSource
import com.mowtiie.mesgard.domain.repository.MfaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AuthModule {
    @Binds
    @Singleton
    fun bindPasswordRemoteSource(source: SupabasePasswordRemoteSource): PasswordRemoteSource

    @Binds
    @Singleton
    fun bindMfaRemoteSource(source: SupabaseMfaRemoteSource): MfaRemoteSource

    @Binds
    @Singleton
    fun bindMfaRepository(repository: SupabaseMfaRepository): MfaRepository
}
