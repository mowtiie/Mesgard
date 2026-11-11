package com.mowtiie.mesgard.data

import androidx.room.withTransaction
import com.mowtiie.mesgard.data.local.MesgardDatabase
import com.mowtiie.mesgard.data.remote.ContactRemoteSource
import com.mowtiie.mesgard.data.remote.RemoteOutcome
import com.mowtiie.mesgard.data.remote.attemptRemote
import com.mowtiie.mesgard.data.remote.toContact
import com.mowtiie.mesgard.data.remote.toEntity
import com.mowtiie.mesgard.data.remote.toRefreshResult
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.model.StartConversationResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.ContactRepository
import com.mowtiie.mesgard.domain.repository.ConversationRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val TAG = "ContactRepository"

internal class OfflineFirstContactRepository @Inject constructor(
    private val database: MesgardDatabase,
    private val remoteSource: ContactRemoteSource,
    private val conversationRepository: ConversationRepository,
    private val authRepository: AuthRepository,
) : ContactRepository {
    private val contactDao = database.contactDao()

    override fun observeContacts(): Flow<List<Contact>> =
        contactDao.observeAll().map { contacts -> contacts.map { it.toContact() } }

    override suspend fun refresh(): RefreshResult = attemptRemote(TAG) {
        val contacts = remoteSource.fetchContacts(authRepository.currentUserId).map { it.toEntity() }
        database.withTransaction {
            contactDao.deleteAll()
            contactDao.upsertAll(contacts)
        }
    }.toRefreshResult()

    override suspend fun startDirectConversation(contact: Contact): StartConversationResult {
        val created = attemptRemote(TAG) { remoteSource.createDirectConversation(contact.orgId, contact.userId) }
        return when (created) {
            is RemoteOutcome.Failure -> StartConversationResult.Failure(created.error)
            is RemoteOutcome.Success -> openCreated(created.value)
        }
    }

    private suspend fun openCreated(conversationId: String): StartConversationResult =
        when (val refreshed = conversationRepository.refresh()) {
            RefreshResult.Success -> StartConversationResult.Started(conversationId)
            is RefreshResult.Failure -> StartConversationResult.Failure(refreshed.error)
        }
}
