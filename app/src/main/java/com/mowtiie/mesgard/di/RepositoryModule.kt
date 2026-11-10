package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.OfflineFirstConversationRepository
import com.mowtiie.mesgard.data.auth.SupabaseAuthRepository
import com.mowtiie.mesgard.data.local.LocalDataEraser
import com.mowtiie.mesgard.data.local.RoomLocalDataEraser
import com.mowtiie.mesgard.data.remote.ConversationRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseConversationRemoteSource
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.ConversationRepository
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

    @Binds
    @Singleton
    abstract fun bindConversationRepository(repository: OfflineFirstConversationRepository): ConversationRepository

    @Binds
    abstract fun bindConversationRemoteSource(source: SupabaseConversationRemoteSource): ConversationRemoteSource

    @Binds
    abstract fun bindLocalDataEraser(eraser: RoomLocalDataEraser): LocalDataEraser
}
