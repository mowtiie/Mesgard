package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.MfaEnrollment
import com.mowtiie.mesgard.domain.model.MfaResult
import kotlinx.coroutines.flow.Flow

internal interface MfaRepository {
    /** Whether the signed in person has a confirmed second factor. */
    val isEnabled: Flow<Boolean>

    /** Removes a factor left unconfirmed by an earlier attempt and creates a new one. */
    suspend fun startEnrollment(): MfaResult<MfaEnrollment>

    /** Confirms the first code of a new factor. The session becomes aal2 on success. */
    suspend fun confirmEnrollment(enrollment: MfaEnrollment, code: String): MfaResult<Unit>

    /** Checks the code of the verified factor of the signed in person. The session becomes aal2 on success. */
    suspend fun verifySignIn(code: String): MfaResult<Unit>

    /** Removes the second factor of the signed in person. Where the server requires one, setup is asked again. */
    suspend fun disable(): MfaResult<Unit>
}
