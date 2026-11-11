package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.push.AndroidMessageNotifier
import com.mowtiie.mesgard.data.push.MessageNotifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NotificationModule {
    @Binds
    abstract fun bindMessageNotifier(notifier: AndroidMessageNotifier): MessageNotifier
}
