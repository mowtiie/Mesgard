package com.mowtiie.mesgard.di

import android.content.Context
import androidx.room.Room
import com.mowtiie.mesgard.data.local.ConversationDao
import com.mowtiie.mesgard.data.local.MesgardDatabase
import com.mowtiie.mesgard.data.local.MessageDao
import com.mowtiie.mesgard.data.local.ProfileDao
import com.mowtiie.mesgard.data.local.security.DatabasePassphraseProvider
import com.mowtiie.mesgard.data.local.security.EncryptedFile
import com.mowtiie.mesgard.data.local.security.SecretCipher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

private const val SQLCIPHER_LIBRARY = "sqlcipher"
private const val PASSPHRASE_FILE_NAME = "database.key"

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabasePassphraseProvider(
        @ApplicationContext context: Context,
        cipher: SecretCipher,
    ): DatabasePassphraseProvider =
        DatabasePassphraseProvider(EncryptedFile(File(context.noBackupFilesDir, PASSPHRASE_FILE_NAME), cipher))

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        passphraseProvider: DatabasePassphraseProvider,
    ): MesgardDatabase {
        System.loadLibrary(SQLCIPHER_LIBRARY)
        return Room.databaseBuilder(context, MesgardDatabase::class.java, MesgardDatabase.NAME)
            .openHelperFactory(SupportOpenHelperFactory(passphraseProvider.getOrCreate()))
            .build()
    }

    @Provides
    fun provideConversationDao(database: MesgardDatabase): ConversationDao = database.conversationDao()

    @Provides
    fun provideProfileDao(database: MesgardDatabase): ProfileDao = database.profileDao()

    @Provides
    fun provideMessageDao(database: MesgardDatabase): MessageDao = database.messageDao()
}
