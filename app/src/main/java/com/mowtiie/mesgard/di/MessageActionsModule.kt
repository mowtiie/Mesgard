package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.OfflineFirstMessageActionsRepository
import com.mowtiie.mesgard.domain.repository.MessageActionsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface MessageActionsModule {
    @Binds
    @Singleton
    fun bindMessageActionsRepository(repository: OfflineFirstMessageActionsRepository): MessageActionsRepository
}
