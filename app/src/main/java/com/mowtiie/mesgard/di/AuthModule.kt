package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.remote.PasswordRemoteSource
import com.mowtiie.mesgard.data.remote.SupabasePasswordRemoteSource
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
}
