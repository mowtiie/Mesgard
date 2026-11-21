package com.mowtiie.mesgard.data

import android.content.Context
import com.mowtiie.mesgard.data.attachment.SHARED_ATTACHMENT_DIRECTORY
import com.mowtiie.mesgard.data.remote.DataExportRemoteSource
import com.mowtiie.mesgard.data.remote.ExportCursor
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.repository.DataExportRepository
import com.mowtiie.mesgard.domain.repository.EXPORT_PAGE_SIZE
import com.mowtiie.mesgard.domain.repository.ExportFile
import com.mowtiie.mesgard.domain.repository.ExportResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.Writer
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val EXPORT_NOTES = listOf(
    "Messages sent by other people are not included, because they are the data of those people.",
    "Reports and legal holds are not included. The Data Protection Officer handles them.",
    "Files are described by name, type and size. The files themselves are not included.",
)

private val PrettyJson = Json { prettyPrint = true }

/**
 * Writes the export to a file in the cache, one page of messages at a time, so a long history never has to fit in
 * memory. The file is removed when the person signs out, like the files shared from conversations.
 */
internal class RemoteDataExportRepository @Inject constructor(
    private val remote: DataExportRemoteSource,
    @ApplicationContext private val context: Context,
    private val clock: Clock,
) : DataExportRepository {
    override suspend fun export(orgId: String, userId: String, personName: String): ExportResult {
        val target = exportFile(personName)
        val result = attemptAdmin { withContext(Dispatchers.IO) { write(target, orgId, userId) } }
        return when (result) {
            is AdminResult.Success -> ExportResult.Ready(ExportFile(target.absolutePath, target.name))
            is AdminResult.Failure -> {
                target.delete()
                ExportResult.Failure(result.error)
            }
        }
    }

    private fun exportFile(personName: String): File {
        val directory = File(context.cacheDir, SHARED_ATTACHMENT_DIRECTORY).also { it.mkdirs() }
        val safeName = personName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(directory, "mesgard-export-$safeName-${LocalDate.now(clock)}.json")
    }

    private suspend fun write(target: File, orgId: String, userId: String) {
        val overview = remote.overview(orgId, userId)
        target.bufferedWriter().use { out ->
            out.write("{\n")
            overview.forEach { (key, value) -> out.writeEntry(key, value) }
            out.writeEntry("notes", JsonArray(EXPORT_NOTES.map(::JsonPrimitive)))
            out.write("  \"messages\": [")
            writeMessages(out, orgId, userId)
            out.write("\n  ]\n}\n")
        }
    }

    private suspend fun writeMessages(out: Writer, orgId: String, userId: String) {
        var cursor: ExportCursor? = null
        var isFirst = true
        do {
            val page = remote.messages(orgId, userId, cursor, EXPORT_PAGE_SIZE)
            page.forEach { message ->
                out.write(if (isFirst) "\n" else ",\n")
                isFirst = false
                out.write(PrettyJson.encodeToString(JsonElement.serializer(), message))
            }
            cursor = page.lastOrNull()?.let(::cursorAfter)
        } while (page.size == EXPORT_PAGE_SIZE)
    }
}

private fun cursorAfter(message: JsonElement): ExportCursor {
    val fields = message.jsonObject
    return ExportCursor(
        createdAt = fields.getValue("created_at").jsonPrimitive.content,
        messageId = fields.getValue("message_id").jsonPrimitive.content,
    )
}

private fun Writer.writeEntry(key: String, value: JsonElement) {
    write("  ${JsonPrimitive(key)}: ${PrettyJson.encodeToString(JsonElement.serializer(), value)},\n")
}
