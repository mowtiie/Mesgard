package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.HoldTarget
import com.mowtiie.mesgard.domain.model.LegalHold

/** Legal holds of an organization. Only organization admins can use them, and they need a connection. */
internal interface LegalHoldRepository {
    /** The holds that are active now. A released hold stays in the history on the server. */
    suspend fun activeHolds(orgId: String): AdminResult<List<LegalHold>>

    suspend fun place(orgId: String, target: HoldTarget, reason: String): AdminResult<Unit>

    suspend fun release(holdId: String, note: String): AdminResult<Unit>
}
