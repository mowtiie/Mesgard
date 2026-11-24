package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.RemoteProfilePictureRepository
import com.mowtiie.mesgard.data.avatar.BitmapAvatarPhotos
import com.mowtiie.mesgard.data.avatar.OfflineFirstAvatarSource
import com.mowtiie.mesgard.data.remote.AvatarRemoteSource
import com.mowtiie.mesgard.data.remote.ProfilePictureRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseAvatarRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseProfilePictureRemoteSource
import com.mowtiie.mesgard.domain.avatar.AvatarPhotos
import com.mowtiie.mesgard.domain.avatar.AvatarSource
import com.mowtiie.mesgard.domain.repository.ProfilePictureRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AvatarModule {
    @Binds
    @Singleton
    fun bindAvatarRemoteSource(source: SupabaseAvatarRemoteSource): AvatarRemoteSource

    @Binds
    @Singleton
    fun bindAvatarSource(source: OfflineFirstAvatarSource): AvatarSource

    @Binds
    @Singleton
    fun bindAvatarPhotos(photos: BitmapAvatarPhotos): AvatarPhotos

    @Binds
    @Singleton
    fun bindProfilePictureRemoteSource(source: SupabaseProfilePictureRemoteSource): ProfilePictureRemoteSource

    @Binds
    @Singleton
    fun bindProfilePictureRepository(repository: RemoteProfilePictureRepository): ProfilePictureRepository
}
