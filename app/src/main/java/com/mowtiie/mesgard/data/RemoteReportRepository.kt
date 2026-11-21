package com.mowtiie.mesgard.data

import android.util.Log
import com.mowtiie.mesgard.data.local.MesgardDatabase
import com.mowtiie.mesgard.data.remote.ReportRemoteSource
import com.mowtiie.mesgard.data.remote.SubmitReportRequest
import com.mowtiie.mesgard.domain.model.MessageReport
import com.mowtiie.mesgard.domain.model.ReportCategory
import com.mowtiie.mesgard.domain.model.ReportError
import com.mowtiie.mesgard.domain.model.ReportResult
import com.mowtiie.mesgard.domain.repository.ReportRepository
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import java.io.IOException
import javax.inject.Inject

private const val TAG = "ReportRepository"
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val HTTP_CONFLICT = 409

internal class RemoteReportRepository @Inject constructor(
    private val remote: ReportRemoteSource,
    database: MesgardDatabase,
) : ReportRepository {
    private val conversationDao = database.conversationDao()

    override suspend fun reportMessage(report: MessageReport): ReportResult {
        val orgId = conversationDao.findOrgId(report.conversationId)
            ?: return ReportResult.Failure(ReportError.UNKNOWN)
        val request = SubmitReportRequest(
            reportId = report.reportId,
            orgId = orgId,
            category = report.category.wireName(),
            messageId = report.messageId,
            note = report.note.trim().ifEmpty { null },
            isAnonymous = report.isAnonymous,
        )
        return try {
            remote.submit(request)
            ReportResult.Sent
        } catch (exception: HttpRequestException) {
            Log.i(TAG, "The server could not be reached", exception)
            ReportResult.Failure(ReportError.NETWORK_UNAVAILABLE)
        } catch (exception: IOException) {
            Log.i(TAG, "The server could not be reached", exception)
            ReportResult.Failure(ReportError.NETWORK_UNAVAILABLE)
        } catch (exception: RestException) {
            Log.w(TAG, "The server refused the report", exception)
            ReportResult.Failure(reportErrorFor(exception.statusCode))
        } catch (exception: IllegalArgumentException) {
            Log.w(TAG, "The report is not valid", exception)
            ReportResult.Failure(ReportError.UNKNOWN)
        }
    }
}

internal fun reportErrorFor(statusCode: Int): ReportError = when (statusCode) {
    HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> ReportError.NOT_ALLOWED
    HTTP_CONFLICT -> ReportError.ALREADY_REPORTED
    else -> ReportError.UNKNOWN
}

internal fun ReportCategory.wireName(): String = when (this) {
    ReportCategory.BULLYING -> "bullying"
    ReportCategory.HARASSMENT -> "harassment"
    ReportCategory.CHILD_SAFETY -> "child_safety"
    ReportCategory.OTHER -> "other"
}
