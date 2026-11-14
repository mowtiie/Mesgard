package com.mowtiie.mesgard.domain.admin

/** Reads and writes the files a person picks with the system file picker. */
internal interface DocumentStore {
    /** The text of the file, or null when it cannot be read or is larger than [maxBytes]. */
    suspend fun readText(uri: String, maxBytes: Int): String?

    suspend fun writeText(uri: String, text: String): Boolean
}
