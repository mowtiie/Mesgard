package com.mowtiie.mesgard.data.local

import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal interface LocalDataEraser {
    suspend fun eraseAll()
}

internal class RoomLocalDataEraser @Inject constructor(private val database: MesgardDatabase) : LocalDataEraser {
    override suspend fun eraseAll() = withContext(Dispatchers.IO) {
        database.clearAllTables()
    }
}
