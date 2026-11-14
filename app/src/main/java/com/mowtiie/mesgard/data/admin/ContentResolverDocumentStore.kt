package com.mowtiie.mesgard.data.admin

import android.content.Context
import android.net.Uri
import com.mowtiie.mesgard.domain.admin.DocumentStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ContentResolverDocumentStore @Inject constructor(@ApplicationContext private val context: Context) :
    DocumentStore {
    override suspend fun readText(uri: String, maxBytes: Int): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(Uri.parse(uri))?.use { stream ->
                readAtMost(stream, maxBytes)?.toString(Charsets.UTF_8)
            }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    override suspend fun writeText(uri: String, text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(Uri.parse(uri), "wt")?.use { it.write(text.toByteArray()) } != null
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}

/** The content of the stream, or null when it has more than [limit] bytes. */
private fun readAtMost(stream: InputStream, limit: Int): ByteArray? {
    val buffer = ByteArray(limit + 1)
    var total = 0
    while (total < buffer.size) {
        val read = stream.read(buffer, total, buffer.size - total)
        if (read < 0) break
        total += read
    }
    return if (total > limit) null else buffer.copyOf(total)
}
