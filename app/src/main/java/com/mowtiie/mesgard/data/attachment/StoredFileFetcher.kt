package com.mowtiie.mesgard.data.attachment

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.request.Options
import java.io.File
import okio.Buffer
import okio.FileSystem

/** A stored attachment file as an image model. It may be sealed, so the image loader reads it through the vault. */
internal data class StoredFile(val path: String)

internal class StoredFileKeyer : Keyer<StoredFile> {
    override fun key(data: StoredFile, options: Options): String = data.path
}

internal class StoredFileFetcher(private val data: StoredFile, private val vault: AttachmentVault) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val bytes = vault.readPlain(File(data.path))
        return SourceFetchResult(
            source = ImageSource(source = Buffer().write(bytes), fileSystem = FileSystem.SYSTEM),
            mimeType = null,
            dataSource = DataSource.DISK,
        )
    }

    class Factory(private val vault: AttachmentVault) : Fetcher.Factory<StoredFile> {
        override fun create(data: StoredFile, options: Options, imageLoader: ImageLoader): Fetcher =
            StoredFileFetcher(data, vault)
    }
}
