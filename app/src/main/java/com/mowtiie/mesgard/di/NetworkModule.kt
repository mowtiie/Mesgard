package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.network.AndroidConnectivityMonitor
import com.mowtiie.mesgard.domain.network.ConnectivityMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NetworkModule {
    @Binds
    abstract fun bindConnectivityMonitor(monitor: AndroidConnectivityMonitor): ConnectivityMonitor
}
