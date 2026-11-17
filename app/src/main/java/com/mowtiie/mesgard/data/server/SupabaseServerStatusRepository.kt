package com.mowtiie.mesgard.data.server

import android.util.Log
import com.mowtiie.mesgard.data.remote.ServerInfoRemoteSource
import com.mowtiie.mesgard.domain.model.ServerStatus
import com.mowtiie.mesgard.domain.repository.ServerStatusRepository
import com.mowtiie.mesgard.domain.server.serverStatusFor
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import java.io.IOException
import javax.inject.Inject
import kotlinx.serialization.SerializationException

private const val TAG = "ServerStatusRepository"
private const val HTTP_NOT_FOUND = 404

internal class SupabaseServerStatusRepository @Inject constructor(private val remoteSource: ServerInfoRemoteSource) :
    ServerStatusRepository {
    override suspend fun check(): ServerStatus = try {
        serverStatusFor(remoteSource.readInfo().apiLevel)
    } catch (exception: RestException) {
        statusForRefusal(exception)
    } catch (exception: HttpRequestException) {
        unknown("The server could not be reached", exception)
    } catch (exception: IOException) {
        unknown("The server could not be reached", exception)
    } catch (exception: SerializationException) {
        unknown("The server answered with something unexpected", exception)
    }

    /** A server without the function answers 404, which is how a server from before this check looks. */
    private fun statusForRefusal(exception: RestException): ServerStatus = if (exception.statusCode == HTTP_NOT_FOUND) {
        ServerStatus.OUTDATED
    } else {
        unknown("The server refused the status request", exception)
    }

    private fun unknown(message: String, exception: Exception): ServerStatus {
        Log.i(TAG, message, exception)
        return ServerStatus.UNKNOWN
    }
}
