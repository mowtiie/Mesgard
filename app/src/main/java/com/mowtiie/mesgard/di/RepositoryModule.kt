package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.OfflineFirstContactRepository
import com.mowtiie.mesgard.data.OfflineFirstConversationRepository
import com.mowtiie.mesgard.data.OfflineFirstMessageRepository
import com.mowtiie.mesgard.data.auth.SupabaseAuthRepository
import com.mowtiie.mesgard.data.local.LocalDataEraser
import com.mowtiie.mesgard.data.local.RoomLocalDataEraser
import com.mowtiie.mesgard.data.lock.TimedAppLock
import com.mowtiie.mesgard.data.sync.RealtimeMessageSync
import com.mowtiie.mesgard.data.work.MessageSendScheduler
import com.mowtiie.mesgard.data.work.WorkManagerMessageSendScheduler
import com.mowtiie.mesgard.domain.lock.AppLock
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.ContactRepository
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
    @Singleton
    abstract fun bindMessageRepository(repository: OfflineFirstMessageRepository): MessageRepository

    @Binds
    @Singleton
    abstract fun bindContactRepository(repository: OfflineFirstContactRepository): ContactRepository

    @Binds
    abstract fun bindMessageSendScheduler(scheduler: WorkManagerMessageSendScheduler): MessageSendScheduler

    @Binds
    abstract fun bindRealtimeSync(sync: RealtimeMessageSync): RealtimeSync

    @Binds
    abstract fun bindAppLock(lock: TimedAppLock): AppLock

    @Binds
    abstract fun bindLocalDataEraser(eraser: RoomLocalDataEraser): LocalDataEraser
}
