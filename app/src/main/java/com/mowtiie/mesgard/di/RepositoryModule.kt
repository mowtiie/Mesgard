package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.OfflineFirstConversationRepository
import com.mowtiie.mesgard.data.OfflineFirstMessageRepository
import com.mowtiie.mesgard.data.auth.SupabaseAuthRepository
import com.mowtiie.mesgard.data.local.LocalDataEraser
import com.mowtiie.mesgard.data.local.RoomLocalDataEraser
import com.mowtiie.mesgard.data.remote.ConversationRemoteSource
import com.mowtiie.mesgard.data.remote.MessageRealtimeSource
import com.mowtiie.mesgard.data.remote.MessageRemoteSource
import com.mowtiie.mesgard.data.remote.ProfileRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseConversationRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseMessageRealtimeSource
import com.mowtiie.mesgard.data.remote.SupabaseMessageRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseProfileRemoteSource
import com.mowtiie.mesgard.data.sync.RealtimeMessageSync
import com.mowtiie.mesgard.data.work.MessageSendScheduler
import com.mowtiie.mesgard.data.work.WorkManagerMessageSendScheduler
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.ConversationRepository
import com.mowtiie.mesgard.domain.repository.MessageRepository
import com.mowtiie.mesgard.domain.sync.RealtimeSync
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
    @Singleton
    abstract fun bindMessageRepository(repository: OfflineFirstMessageRepository): MessageRepository

    @Binds
    abstract fun bindMessageRemoteSource(source: SupabaseMessageRemoteSource): MessageRemoteSource

    @Binds
    abstract fun bindMessageSendScheduler(scheduler: WorkManagerMessageSendScheduler): MessageSendScheduler

    @Binds
    abstract fun bindMessageRealtimeSource(source: SupabaseMessageRealtimeSource): MessageRealtimeSource

    @Binds
    abstract fun bindProfileRemoteSource(source: SupabaseProfileRemoteSource): ProfileRemoteSource

    @Binds
    abstract fun bindRealtimeSync(sync: RealtimeMessageSync): RealtimeSync

    @Binds
    abstract fun bindLocalDataEraser(eraser: RoomLocalDataEraser): LocalDataEraser
}
