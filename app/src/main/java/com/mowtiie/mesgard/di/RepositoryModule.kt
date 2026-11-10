package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.auth.SupabaseAuthRepository
import com.mowtiie.mesgard.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(repository: SupabaseAuthRepository): AuthRepository
}
