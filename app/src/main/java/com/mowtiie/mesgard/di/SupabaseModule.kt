package com.mowtiie.mesgard.di

import android.content.Context
import com.mowtiie.mesgard.BuildConfig
import com.mowtiie.mesgard.data.auth.EncryptedSessionManager
import com.mowtiie.mesgard.data.local.security.EncryptedFile
import com.mowtiie.mesgard.data.local.security.SecretCipher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import java.io.File
import javax.inject.Singleton

private const val SESSION_FILE_NAME = "session.bin"

@Module
@InstallIn(SingletonComponent::class)
internal object SupabaseModule {
    @Provides
    @Singleton
    fun provideSessionManager(@ApplicationContext context: Context, cipher: SecretCipher): SessionManager =
        EncryptedSessionManager(EncryptedFile(File(context.noBackupFilesDir, SESSION_FILE_NAME), cipher))

    @Provides
    @Singleton
    fun provideSupabaseClient(sessionManager: SessionManager): SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
    ) {
        install(Auth) {
            this.sessionManager = sessionManager
        }
        install(Postgrest)
        install(Realtime)
        install(Storage)
    }

    @Provides
    @Singleton
    fun provideAuth(client: SupabaseClient): Auth = client.auth
}
