package com.mowtiie.mesgard.data.local

import app.cash.turbine.test
import com.mowtiie.mesgard.testing.InMemoryDatabaseRule
import com.mowtiie.mesgard.testing.groupEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GroupDaoTest {
    @get:Rule
    val databaseRule = InMemoryDatabaseRule()

    private val groupDao get() = databaseRule.database.groupDao()

    @Test
    fun `lists groups alphabetically ignoring case`() = runTest {
        groupDao.upsertAll(
            listOf(
                groupEntity("1", name = "science"),
                groupEntity("2", name = "Grade 10"),
                groupEntity("3", name = "Music"),
            ),
        )

        groupDao.observeAll().test {
            assertEquals(listOf("Grade 10", "Music", "science"), awaitItem().map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `upsert replaces a group with the same id`() = runTest {
        groupDao.upsertAll(listOf(groupEntity("1", name = "Old name")))
        groupDao.upsertAll(listOf(groupEntity("1", name = "New name")))

        groupDao.observeAll().test {
            assertEquals(listOf("New name"), awaitItem().map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteAll removes every group`() = runTest {
        groupDao.upsertAll(listOf(groupEntity("1"), groupEntity("2")))

        groupDao.deleteAll()

        groupDao.observeAll().test {
            assertEquals(emptyList<GroupEntity>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
