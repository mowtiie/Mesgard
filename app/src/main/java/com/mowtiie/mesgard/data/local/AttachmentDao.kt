package com.mowtiie.mesgard.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface AttachmentDao {
    @Upsert
    suspend fun upsertAll(attachments: List<AttachmentEntity>)

    @Query("SELECT * FROM attachments WHERE id IN (:ids)")
    suspend fun findByIds(ids: List<String>): List<AttachmentEntity>

    @Query("SELECT * FROM attachments WHERE message_id = :messageId ORDER BY file_name, id")
    suspend fun findByMessage(messageId: String): List<AttachmentEntity>

    @Query(
        """
        SELECT a.* FROM attachments AS a
        JOIN messages AS m ON m.id = a.message_id
        WHERE m.conversation_id = :conversationId
        ORDER BY a.file_name, a.id
        """,
    )
    fun observeForConversation(conversationId: String): Flow<List<AttachmentEntity>>

    @Query("UPDATE attachments SET local_path = :localPath WHERE id = :attachmentId")
    suspend fun updateLocalPath(attachmentId: String, localPath: String)

    /** Files on the phone that belong to sent messages, so they can be downloaded again, the oldest message first. */
    @Query(
        """
        SELECT a.* FROM attachments AS a
        JOIN messages AS m ON m.id = a.message_id
        WHERE a.local_path IS NOT NULL AND m.status = 'SENT'
        ORDER BY m.created_at ASC, a.id ASC
        """,
    )
    suspend fun findStoredOldestFirst(): List<AttachmentEntity>

    @Query("UPDATE attachments SET local_path = NULL WHERE id = :attachmentId")
    suspend fun clearLocalPath(attachmentId: String)

    @Query(
        """
        UPDATE attachments SET local_path = :localPath, size_bytes = :sizeBytes, width = :width, height = :height
        WHERE id = :attachmentId
        """,
    )
    suspend fun updateFile(attachmentId: String, localPath: String, sizeBytes: Long, width: Int?, height: Int?)
}

internal suspend fun AttachmentDao.upsertKeepingLocalPaths(attachments: List<AttachmentEntity>) {
    if (attachments.isEmpty()) return
    val localPaths = findByIds(attachments.map { it.id }).associate { it.id to it.localPath }
    upsertAll(attachments.map { it.copy(localPath = localPaths[it.id]) })
}
