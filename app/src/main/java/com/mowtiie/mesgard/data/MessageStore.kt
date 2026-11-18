package com.mowtiie.mesgard.data

import androidx.room.withTransaction
import com.mowtiie.mesgard.data.local.MesgardDatabase
import com.mowtiie.mesgard.data.local.upsertKeepingLocalPaths
import com.mowtiie.mesgard.data.remote.MessageDto
import com.mowtiie.mesgard.data.remote.toEntity
import com.mowtiie.mesgard.data.remote.toSenderProfile

/** Stores messages from the server with their senders and attachments, keeping files already on the device. */
internal suspend fun MesgardDatabase.storeMessages(messages: List<MessageDto>) {
    withTransaction {
        profileDao().upsertAll(messages.mapNotNull { it.toSenderProfile() }.distinctBy { it.id })
        messageDao().upsertAll(messages.map { it.toEntity() })
        attachmentDao().upsertKeepingLocalPaths(
            messages.flatMap { dto ->
                dto.attachments.map { it.toEntity(dto.id) }
            },
        )
    }
}
