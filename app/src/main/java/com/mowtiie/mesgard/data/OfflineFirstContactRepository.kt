package com.mowtiie.mesgard.data

import androidx.room.withTransaction
import com.mowtiie.mesgard.data.local.ConversationEntity
import com.mowtiie.mesgard.data.local.DirectPeerEntity
import com.mowtiie.mesgard.data.local.MesgardDatabase
import com.mowtiie.mesgard.data.remote.ContactRemoteSource
import com.mowtiie.mesgard.data.remote.RemoteOutcome
import com.mowtiie.mesgard.data.remote.attemptRemote
import com.mowtiie.mesgard.data.remote.toContact
import com.mowtiie.mesgard.data.remote.toEntity
import com.mowtiie.mesgard.data.remote.toRefreshResult
import com.mowtiie.mesgard.data.remote.toStoredEntity
import com.mowtiie.mesgard.di.ApplicationScope
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.ContactSearchResult
import com.mowtiie.mesgard.domain.model.ConversationType
import com.mowtiie.mesgard.domain.model.MAX_FAVORITE_CONTACTS
import com.mowtiie.mesgard.domain.model.PreferenceError
import com.mowtiie.mesgard.domain.model.PreferenceResult
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.model.StartConversationResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.ContactRepository
import com.mowtiie.mesgard.domain.repository.ConversationRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val TAG = "ContactRepository"

internal class OfflineFirstContactRepository @Inject constructor(
    private val database: MesgardDatabase,
    private val remoteSource: ContactRemoteSource,
    private val conversationRepository: ConversationRepository,
    private val authRepository: AuthRepository,
    @ApplicationScope private val scope: CoroutineScope,
) : ContactRepository {
    private val contactDao = database.contactDao()
    private val conversationDao = database.conversationDao()
    private val directPeerDao = database.directPeerDao()

    override fun observeContacts(): Flow<List<Contact>> =
        contactDao.observeAll().map { contacts -> contacts.map { it.toContact() } }

    override suspend fun refresh(): RefreshResult = attemptRemote(TAG) {
        val userId = authRepository.currentUserId
        val favoriteIds = remoteSource.fetchFavoriteIds(userId)
        val roster = remoteSource.fetchContacts(userId)
        // A favorite stays on the list even when the roster of a large organization does not reach that far.
        val missing = favoriteIds - roster.mapTo(mutableSetOf()) { it.userId }
        val contacts = (roster + remoteSource.fetchContactsByIds(missing))
            .map { it.toEntity(isFavorite = it.userId in favoriteIds) }
        database.withTransaction {
            contactDao.deleteAll()
            contactDao.upsertAll(contacts)
        }
    }.toRefreshResult()

    /**
     * A conversation the phone already has opens without the network. A new one opens as soon as the server has made
     * it, from what the phone knows, and the conversation list catches up in the background.
     */
    override suspend fun startDirectConversation(contact: Contact): StartConversationResult {
        directPeerDao.findConversationWith(contact.userId, contact.orgId)?.let {
            return StartConversationResult.Started(it)
        }
        val created = attemptRemote(TAG) { remoteSource.createDirectConversation(contact.orgId, contact.userId) }
        return when (created) {
            is RemoteOutcome.Failure -> StartConversationResult.Failure(created.error)
            is RemoteOutcome.Success -> openCreated(created.value, contact)
        }
    }

    override suspend fun search(query: String): ContactSearchResult {
        val userId = authRepository.currentUserId
        val outcome = attemptRemote(TAG) {
            // The favorites are on the phone already, a person found by searching is marked from them.
            val favoriteIds = contactDao.findFavoriteIds().toSet()
            remoteSource.searchContacts(query, userId).map {
                it.toEntity(isFavorite = it.userId in favoriteIds).toContact()
            }
        }
        return when (outcome) {
            is RemoteOutcome.Success -> ContactSearchResult.Found(outcome.value)
            is RemoteOutcome.Failure -> ContactSearchResult.Unavailable
        }
    }

    override suspend fun setFavorite(contacts: Set<Contact>, favorite: Boolean): PreferenceResult {
        val userId = authRepository.currentUserId
        val changed = contacts.filter { it.isFavorite != favorite }
        return when {
            userId == null -> PreferenceResult.Failure(PreferenceError.UNKNOWN)
            changed.isEmpty() -> PreferenceResult.Success
            favorite && contactDao.countFavorites() + changed.size > MAX_FAVORITE_CONTACTS ->
                PreferenceResult.Failure(PreferenceError.TOO_MANY_FAVORITES)
            else -> applyFavorite(userId, changed, favorite)
        }
    }

    private suspend fun writeFavorites(people: List<Contact>, favorite: Boolean) =
        contactDao.upsertAll(people.map { it.copy(isFavorite = favorite).toStoredEntity() })

    private suspend fun applyFavorite(userId: String, people: List<Contact>, favorite: Boolean): PreferenceResult {
        val idsByOrganization = people.groupBy({ it.orgId }, { it.userId })
        writeFavorites(people, favorite)
        val outcome = attemptRemote(TAG) {
            idsByOrganization.forEach { (orgId, ids) ->
                if (favorite) {
                    remoteSource.addFavorites(
                        orgId,
                        userId,
                        ids,
                    )
                } else {
                    remoteSource.removeFavorites(orgId, userId, ids)
                }
            }
        }
        return when (outcome) {
            is RemoteOutcome.Success -> PreferenceResult.Success
            is RemoteOutcome.Failure -> {
                writeFavorites(people, !favorite)
                PreferenceResult.Failure(outcome.error.toPreferenceError())
            }
        }
    }

    private suspend fun openCreated(conversationId: String, contact: Contact): StartConversationResult {
        database.withTransaction {
            conversationDao.insertIfAbsent(
                ConversationEntity(
                    id = conversationId,
                    orgId = contact.orgId,
                    type = ConversationType.DIRECT,
                    title = contact.displayName,
                    createdAtEpochMillis = System.currentTimeMillis(),
                    unreadCount = 0,
                ),
            )
            directPeerDao.upsertAll(listOf(DirectPeerEntity(conversationId, contact.userId)))
        }
        scope.launch { conversationRepository.refresh() }
        return StartConversationResult.Started(conversationId)
    }
}
