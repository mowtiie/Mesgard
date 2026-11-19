package com.mowtiie.mesgard.data.remote

import com.mowtiie.mesgard.data.local.AttachmentEntity
import com.mowtiie.mesgard.data.local.MessageEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.UploadData
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import io.ktor.util.cio.readChannel
import io.ktor.util.cio.writeChannel
import java.io.File
import javax.inject.Inject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val ATTACHMENT_BUCKET = "message-attachments"
private const val SEND_MESSAGE_FUNCTION = "send_message"

internal interface AttachmentRemoteSource {
    suspend fun upload(storagePath: String, file: File, mimeType: String)

    /** Streams the object into [target], so a large file is never held in memory. A partial file is left on failure. */
    suspend fun download(storagePath: String, target: File)

    /** Stores the message with its already uploaded attachments in one transaction and returns its creation time. */
    suspend fun sendWithAttachments(message: MessageEntity, attachments: List<AttachmentEntity>): String
}

internal class SupabaseAttachmentRemoteSource @Inject constructor(private val client: SupabaseClient) :
    AttachmentRemoteSource {
    override suspend fun upload(storagePath: String, file: File, mimeType: String) {
        client.storage.from(ATTACHMENT_BUCKET).upload(storagePath, UploadData(file.readChannel(), file.length())) {
            upsert = true
            contentType = ContentType.parse(mimeType)
        }
    }

    override suspend fun download(storagePath: String, target: File) {
        val channel = target.writeChannel()
        try {
            client.storage.from(ATTACHMENT_BUCKET).downloadAuthenticated(storagePath, channel)
        } finally {
            channel.flushAndClose()
        }
    }

    override suspend fun sendWithAttachments(message: MessageEntity, attachments: List<AttachmentEntity>): String =
        client.postgrest.rpc(
            SEND_MESSAGE_FUNCTION,
            buildJsonObject {
                put("new_message_id", message.id)
                put("target_conversation_id", message.conversationId)
                put("new_body", message.body)
                message.replyToId?.let { put("new_reply_to", it) }
                message.mentionedUserIds.splitIds().takeIf { it.isNotEmpty() }?.let { ids ->
                    put("new_mentions", buildJsonArray { ids.forEach { add(JsonPrimitive(it)) } })
                }
                put(
                    "new_attachments",
                    buildJsonArray {
                        attachments.forEach {
                            add(
                                buildJsonObject {
                                    put("id", it.id)
                                    put("storage_path", it.storagePath)
                                    put("file_name", it.fileName)
                                    it.width?.let { width -> put("width", width) }
                                    it.height?.let { height -> put("height", height) }
                                },
                            )
                        }
                    },
                )
            },
        ).decodeAs<String>()
}
