package com.mowtiie.mesgard.testing

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mowtiie.mesgard.data.local.MesgardDatabase
import org.junit.rules.ExternalResource

class InMemoryDatabaseRule : ExternalResource() {
    internal lateinit var database: MesgardDatabase
        private set

    override fun before() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MesgardDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    override fun after() {
        database.close()
    }
}
