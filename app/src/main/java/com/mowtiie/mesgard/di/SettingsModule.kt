package com.mowtiie.mesgard.di

import android.content.Context
import com.mowtiie.mesgard.data.local.DEVICE_SETTINGS_FILE
import com.mowtiie.mesgard.data.local.SharedPreferencesDeviceSettings
import com.mowtiie.mesgard.domain.repository.DeviceSettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SettingsModule {
    @Provides
    @Singleton
    fun provideDeviceSettings(@ApplicationContext context: Context): DeviceSettingsRepository =
        SharedPreferencesDeviceSettings(context.getSharedPreferences(DEVICE_SETTINGS_FILE, Context.MODE_PRIVATE))
}
