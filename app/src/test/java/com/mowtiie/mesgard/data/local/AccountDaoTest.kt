package com.mowtiie.mesgard.data.local

import app.cash.turbine.test
import com.mowtiie.mesgard.testing.InMemoryDatabaseRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccountDaoTest {
    @get:Rule
    val databaseRule = InMemoryDatabaseRule()

    private val accountDao get() = databaseRule.database.accountDao()

    @Test
    fun `there is no account before one is stored`() = runTest {
        accountDao.observeAccount().test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stores and replaces the account`() = runTest {
        accountDao.upsertAccount(AccountEntity("u1", "Old name", null))
        accountDao.upsertAccount(AccountEntity("u1", "New name", "ana@school.test"))

        accountDao.observeAccount().test {
            assertEquals(AccountEntity("u1", "New name", "ana@school.test"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `lists memberships alphabetically ignoring case`() = runTest {
        accountDao.upsertMemberships(
            listOf(
                AccountMembershipEntity("o1", "bravo school", "student"),
                AccountMembershipEntity("o2", "Alpha school", "staff"),
            ),
        )

        accountDao.observeMemberships().test {
            assertEquals(listOf("Alpha school", "bravo school"), awaitItem().map { it.orgName })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleting removes the account and its memberships`() = runTest {
        accountDao.upsertAccount(AccountEntity("u1", "Ana", null))
        accountDao.upsertMemberships(listOf(AccountMembershipEntity("o1", "Alpha", "staff")))

        accountDao.deleteAccounts()
        accountDao.deleteMemberships()

        accountDao.observeAccount().test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        accountDao.observeMemberships().test {
            assertEquals(emptyList<AccountMembershipEntity>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
