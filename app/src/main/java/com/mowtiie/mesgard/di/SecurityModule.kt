package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.local.security.KeystoreSecretCipher
import com.mowtiie.mesgard.data.local.security.SecretCipher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SecurityModule {
    @Provides
    @Singleton
    fun provideSecretCipher(): SecretCipher = KeystoreSecretCipher()
}
