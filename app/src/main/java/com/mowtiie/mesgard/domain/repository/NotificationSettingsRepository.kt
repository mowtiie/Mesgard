package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.NotificationPreview
import com.mowtiie.mesgard.domain.model.RefreshResult

internal interface NotificationSettingsRepository {
    suspend fun refresh(): RefreshResult

    suspend fun setPreview(preview: NotificationPreview): RefreshResult
}
