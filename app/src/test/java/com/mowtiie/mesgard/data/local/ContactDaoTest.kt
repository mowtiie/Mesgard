package com.mowtiie.mesgard.data.local

import app.cash.turbine.test
import com.mowtiie.mesgard.testing.InMemoryDatabaseRule
import com.mowtiie.mesgard.testing.contactEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContactDaoTest {
    @get:Rule
    val databaseRule = InMemoryDatabaseRule()

    private val contactDao get() = databaseRule.database.contactDao()

    @Test
    fun `lists contacts alphabetically ignoring case`() = runTest {
        contactDao.upsertAll(
            listOf(
                contactEntity("1", name = "ben"),
                contactEntity("2", name = "Ana"),
                contactEntity("3", name = "Carla"),
            ),
        )

        contactDao.observeAll().test {
            assertEquals(listOf("Ana", "ben", "Carla"), awaitItem().map { it.displayName })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `upsert replaces a contact with the same organization and user`() = runTest {
        contactDao.upsertAll(listOf(contactEntity("1", name = "Old name")))
        contactDao.upsertAll(listOf(contactEntity("1", name = "New name")))

        contactDao.observeAll().test {
            assertEquals(listOf("New name"), awaitItem().map { it.displayName })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteAll removes every contact`() = runTest {
        contactDao.upsertAll(listOf(contactEntity("1"), contactEntity("2")))

        contactDao.deleteAll()

        contactDao.observeAll().test {
            assertEquals(emptyList<ContactEntity>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
