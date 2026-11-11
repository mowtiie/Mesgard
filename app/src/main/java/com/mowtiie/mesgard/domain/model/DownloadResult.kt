package com.mowtiie.mesgard.domain.model

internal sealed interface DownloadResult {
    data class Downloaded(val localPath: String) : DownloadResult

    data class Failure(val error: RefreshError) : DownloadResult
}
