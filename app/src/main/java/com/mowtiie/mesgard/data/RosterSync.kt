package com.mowtiie.mesgard.data

import android.os.SystemClock
import androidx.room.withTransaction
import com.mowtiie.mesgard.data.local.ContactKey
import com.mowtiie.mesgard.data.local.MesgardDatabase
import com.mowtiie.mesgard.data.remote.ContactRemoteSource
import com.mowtiie.mesgard.data.remote.toEntity

private const val ROSTER_FRESH_MS = 5 * 60 * 1_000L

private data class RosterFetch(val userId: String?, val atElapsedMillis: Long)

/**
 * Keeps the stored roster in step with the server. The roster changes rarely and the New chat screen barely lists it,
 * so it is fetched at most every few minutes, while the favorites, which one person changes from several phones, are
 * always fetched.
 */
internal class RosterSync(private val database: MesgardDatabase, private val remoteSource: ContactRemoteSource) {
    private val contactDao = database.contactDao()

    // The signed in user is part of it, so the next user to sign in on this phone never gets the roster skipped.
    private var lastFetch: RosterFetch? = null

    fun isFresh(userId: String?): Boolean {
        val fetch = lastFetch ?: return false
        return fetch.userId == userId && SystemClock.elapsedRealtime() - fetch.atElapsedMillis < ROSTER_FRESH_MS
    }

    suspend fun storeAll(userId: String?, favoriteIds: Set<String>) {
        val roster = remoteSource.fetchContacts(userId)
        // A favorite stays on the list even when the roster of a large organization does not reach that far.
        val missing = favoriteIds - roster.mapTo(mutableSetOf()) { it.userId }
        val contacts = (roster + remoteSource.fetchContactsByIds(missing))
            .map { it.toEntity(isFavorite = it.userId in favoriteIds) }
        // Rows are updated in place and only the people who left are deleted, so nothing is rewritten without a change.
        database.withTransaction {
            contactDao.upsertAll(contacts)
            removePeopleWhoLeft(kept = contacts.map { ContactKey(it.orgId, it.userId) }.toSet())
        }
        lastFetch = RosterFetch(userId, SystemClock.elapsedRealtime())
    }

    /** Applies the favorites to the stored roster, and fetches a favorite the roster does not have. */
    suspend fun storeFavorites(favoriteIds: Set<String>) {
        val missing = favoriteIds - contactDao.findKeys().mapTo(mutableSetOf()) { it.userId }
        val added = remoteSource.fetchContactsByIds(missing).map { it.toEntity(isFavorite = true) }
        database.withTransaction {
            contactDao.upsertAll(added)
            contactDao.setFavorites(favoriteIds.toList())
        }
    }

    private suspend fun removePeopleWhoLeft(kept: Set<ContactKey>) {
        (contactDao.findKeys().toSet() - kept)
            .groupBy({ it.orgId }, { it.userId })
            .forEach { (orgId, userIds) -> contactDao.deleteByUserIds(orgId, userIds) }
    }
}
