package com.mowtiie.mesgard.data

import android.util.Log
import com.mowtiie.mesgard.data.remote.ProfileNoticeRemoteSource
import com.mowtiie.mesgard.domain.repository.ProfileNotice
import com.mowtiie.mesgard.domain.repository.ProfileNoticeKind
import com.mowtiie.mesgard.domain.repository.ProfileNoticeRepository
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import java.io.IOException
import javax.inject.Inject

private const val TAG = "ProfileNoticeRepository"

internal class RemoteProfileNoticeRepository @Inject constructor(private val remote: ProfileNoticeRemoteSource) :
    ProfileNoticeRepository {
    override suspend fun unseen(): List<ProfileNotice> = quietly(emptyList()) {
        remote.unseen().map { ProfileNotice(it.id, noticeKindOf(it.kind)) }
    }

    override suspend fun acknowledge(notices: Collection<ProfileNotice>) {
        if (notices.isEmpty()) return
        quietly(Unit) { remote.acknowledge(notices.map { it.id }) }
    }

    /** A notice is a courtesy, so a failure is logged and the notice simply comes again at the next start. */
    private suspend fun <T> quietly(fallback: T, block: suspend () -> T): T = try {
        block()
    } catch (exception: HttpRequestException) {
        Log.i(TAG, "The server could not be reached", exception)
        fallback
    } catch (exception: IOException) {
        Log.i(TAG, "The server could not be reached", exception)
        fallback
    } catch (exception: RestException) {
        Log.w(TAG, "The server refused the request", exception)
        fallback
    }
}

internal fun noticeKindOf(wireName: String): ProfileNoticeKind =
    if (wireName == "username_reset") ProfileNoticeKind.USERNAME_RESET else ProfileNoticeKind.OTHER
