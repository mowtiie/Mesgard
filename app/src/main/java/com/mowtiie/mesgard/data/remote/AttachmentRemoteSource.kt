package com.mowtiie.mesgard.data.remote

import com.mowtiie.mesgard.data.local.AttachmentEntity
import com.mowtiie.mesgard.data.local.MessageEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import java.io.File
import javax.inject.Inject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val ATTACHMENT_BUCKET = "message-attachments"
private const val SEND_MESSAGE_FUNCTION = "send_message"

internal interface AttachmentRemoteSource {
    suspend fun upload(storagePath: String, file: File, mimeType: String)

    suspend fun download(storagePath: String): ByteArray

    /** Stores the message with its already uploaded attachments in one transaction and returns its creation time. */
    suspend fun sendWithAttachments(message: MessageEntity, attachments: List<AttachmentEntity>): String
}

internal class SupabaseAttachmentRemoteSource @Inject constructor(private val client: SupabaseClient) :
    AttachmentRemoteSource {
    override suspend fun upload(storagePath: String, file: File, mimeType: String) {
        client.storage.from(ATTACHMENT_BUCKET).upload(storagePath, file.readBytes()) {
            upsert = true
            contentType = ContentType.parse(mimeType)
        }
    }

    override suspend fun download(storagePath: String): ByteArray =
        client.storage.from(ATTACHMENT_BUCKET).downloadAuthenticated(storagePath)

    override suspend fun sendWithAttachments(message: MessageEntity, attachments: List<AttachmentEntity>): String =
        client.postgrest.rpc(
            SEND_MESSAGE_FUNCTION,
            buildJsonObject {
                put("new_message_id", message.id)
                put("target_conversation_id", message.conversationId)
                put("new_body", message.body)
                put(
                    "new_attachments",
                    buildJsonArray {
                        attachments.forEach {
                            add(
                                buildJsonObject {
                                    put("id", it.id)
                                    put("storage_path", it.storagePath)
                                    put("file_name", it.fileName)
                                },
                            )
                        }
                    },
                )
            },
        ).decodeAs<String>()
}
