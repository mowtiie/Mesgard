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

    @Query("SELECT local_path FROM attachments WHERE local_path IS NOT NULL")
    suspend fun findAllLocalPaths(): List<String>
}

internal suspend fun AttachmentDao.upsertKeepingLocalPaths(attachments: List<AttachmentEntity>) {
    if (attachments.isEmpty()) return
    val localPaths = findByIds(attachments.map { it.id }).associate { it.id to it.localPath }
    upsertAll(attachments.map { it.copy(localPath = localPaths[it.id]) })
}
