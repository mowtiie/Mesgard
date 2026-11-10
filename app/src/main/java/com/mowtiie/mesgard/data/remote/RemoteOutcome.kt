package com.mowtiie.mesgard.data.remote

import android.util.Log
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import java.io.IOException
import java.time.format.DateTimeParseException

internal sealed interface RemoteOutcome<out T> {
    data class Success<T>(val value: T) : RemoteOutcome<T>

    data class Failure(val error: RefreshError) : RemoteOutcome<Nothing>
}

internal suspend fun <T> attemptRemote(tag: String, block: suspend () -> T): RemoteOutcome<T> = try {
    RemoteOutcome.Success(block())
} catch (exception: HttpRequestException) {
    networkFailure(tag, exception)
} catch (exception: IOException) {
    networkFailure(tag, exception)
} catch (exception: RestException) {
    unknownFailure(tag, exception)
} catch (exception: IllegalArgumentException) {
    unknownFailure(tag, exception)
} catch (exception: DateTimeParseException) {
    unknownFailure(tag, exception)
}

internal fun RemoteOutcome<*>.toRefreshResult(): RefreshResult = when (this) {
    is RemoteOutcome.Success -> RefreshResult.Success
    is RemoteOutcome.Failure -> RefreshResult.Failure(error)
}

private fun networkFailure(tag: String, cause: Exception): RemoteOutcome.Failure {
    Log.i(tag, "The server could not be reached", cause)
    return RemoteOutcome.Failure(RefreshError.NETWORK_UNAVAILABLE)
}

private fun unknownFailure(tag: String, cause: Exception): RemoteOutcome.Failure {
    Log.w(tag, "The request failed", cause)
    return RemoteOutcome.Failure(RefreshError.UNKNOWN)
}
