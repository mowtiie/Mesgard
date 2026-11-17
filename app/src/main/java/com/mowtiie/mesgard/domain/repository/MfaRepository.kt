package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.MfaEnrollment
import com.mowtiie.mesgard.domain.model.MfaResult

internal interface MfaRepository {
    /** Removes a factor left unconfirmed by an earlier attempt and creates a new one. */
    suspend fun startEnrollment(): MfaResult<MfaEnrollment>

    /** Confirms the first code of a new factor. The session becomes aal2 on success. */
    suspend fun confirmEnrollment(enrollment: MfaEnrollment, code: String): MfaResult<Unit>

    /** Checks the code of the verified factor of the signed in person. The session becomes aal2 on success. */
    suspend fun verifySignIn(code: String): MfaResult<Unit>
}
