package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.RemoteConversationGroupRepository
import com.mowtiie.mesgard.data.RemoteGroupAdminRepository
import com.mowtiie.mesgard.data.RemoteGroupAnnouncementRepository
import com.mowtiie.mesgard.data.RemoteGroupTreeRepository
import com.mowtiie.mesgard.data.remote.ConversationGroupRemoteSource
import com.mowtiie.mesgard.data.remote.GroupAdminRemoteSource
import com.mowtiie.mesgard.data.remote.GroupAnnouncementRemoteSource
import com.mowtiie.mesgard.data.remote.GroupTreeRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseConversationGroupRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseGroupAdminRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseGroupAnnouncementRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseGroupTreeRemoteSource
import com.mowtiie.mesgard.domain.repository.ConversationGroupRepository
import com.mowtiie.mesgard.domain.repository.GroupAdminRepository
import com.mowtiie.mesgard.domain.repository.GroupAnnouncementRepository
import com.mowtiie.mesgard.domain.repository.GroupTreeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface GroupAdminModule {
    @Binds
    @Singleton
    fun bindGroupAdminRemoteSource(source: SupabaseGroupAdminRemoteSource): GroupAdminRemoteSource

    @Binds
    @Singleton
    fun bindGroupAdminRepository(repository: RemoteGroupAdminRepository): GroupAdminRepository

    @Binds
    @Singleton
    fun bindGroupAnnouncementRemoteSource(source: SupabaseGroupAnnouncementRemoteSource): GroupAnnouncementRemoteSource

    @Binds
    @Singleton
    fun bindGroupAnnouncementRepository(repository: RemoteGroupAnnouncementRepository): GroupAnnouncementRepository

    @Binds
    @Singleton
    fun bindGroupTreeRemoteSource(source: SupabaseGroupTreeRemoteSource): GroupTreeRemoteSource

    @Binds
    @Singleton
    fun bindGroupTreeRepository(repository: RemoteGroupTreeRepository): GroupTreeRepository

    @Binds
    @Singleton
    fun bindConversationGroupRemoteSource(source: SupabaseConversationGroupRemoteSource): ConversationGroupRemoteSource

    @Binds
    @Singleton
    fun bindConversationGroupRepository(repository: RemoteConversationGroupRepository): ConversationGroupRepository
}
