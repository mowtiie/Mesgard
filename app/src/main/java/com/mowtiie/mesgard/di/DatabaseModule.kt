package com.mowtiie.mesgard.di

import android.content.Context
import androidx.room.Room
import com.mowtiie.mesgard.data.local.MesgardDatabase
import com.mowtiie.mesgard.data.local.MessageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MesgardDatabase =
        Room.databaseBuilder(context, MesgardDatabase::class.java, MesgardDatabase.NAME).build()

    @Provides
    fun provideMessageDao(database: MesgardDatabase): MessageDao = database.messageDao()
}
