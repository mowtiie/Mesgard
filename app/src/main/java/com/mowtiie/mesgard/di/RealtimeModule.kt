package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.RealtimeTypingRepository
import com.mowtiie.mesgard.data.sync.RealtimeMessageSync
import com.mowtiie.mesgard.data.sync.RealtimePresenceTracker
import com.mowtiie.mesgard.domain.presence.PresenceTracker
import com.mowtiie.mesgard.domain.repository.TypingRepository
import com.mowtiie.mesgard.domain.sync.RealtimeSync
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RealtimeModule {
    @Binds
    abstract fun bindRealtimeSync(sync: RealtimeMessageSync): RealtimeSync

    @Binds
    abstract fun bindPresenceTracker(tracker: RealtimePresenceTracker): PresenceTracker

    @Binds
    abstract fun bindTypingRepository(repository: RealtimeTypingRepository): TypingRepository
}
