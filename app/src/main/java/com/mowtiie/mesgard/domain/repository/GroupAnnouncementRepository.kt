package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.AdminResult

/**
 * Sends one text message to a group and to every group inside it. It needs a connection and is not kept on the phone
 * until it was sent. Organization admins and the owners and moderators of the group can send.
 */
internal interface GroupAnnouncementRepository {
    /** The number of conversations the message reached. [messageId] makes a repeated send harmless. */
    suspend fun send(groupId: String, messageId: String, body: String): AdminResult<Int>
}
