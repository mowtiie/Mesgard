package com.mowtiie.mesgard.data.local

import android.content.Context
import com.mowtiie.mesgard.data.attachment.ATTACHMENT_DIRECTORY
import com.mowtiie.mesgard.data.attachment.SHARED_ATTACHMENT_DIRECTORY
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal interface LocalDataEraser {
    suspend fun eraseAll()
}

internal class RoomLocalDataEraser @Inject constructor(
    private val database: MesgardDatabase,
    @ApplicationContext private val context: Context,
) : LocalDataEraser {
    override suspend fun eraseAll() = withContext(Dispatchers.IO) {
        database.clearAllTables()
        File(context.noBackupFilesDir, ATTACHMENT_DIRECTORY).deleteRecursively()
        File(context.cacheDir, SHARED_ATTACHMENT_DIRECTORY).deleteRecursively()
        Unit
    }
}
