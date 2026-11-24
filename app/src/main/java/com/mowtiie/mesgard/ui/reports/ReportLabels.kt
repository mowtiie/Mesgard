package com.mowtiie.mesgard.ui.reports

import android.text.format.DateUtils
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.ReportCategory
import com.mowtiie.mesgard.domain.model.ReportEventType
import com.mowtiie.mesgard.domain.model.ReportStatus
import com.mowtiie.mesgard.domain.model.ReportSubject

private const val DATE_TIME_FLAGS =
    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH

@StringRes
internal fun ReportCategory.labelResource(): Int = when (this) {
    ReportCategory.BULLYING -> R.string.report_category_bullying
    ReportCategory.HARASSMENT -> R.string.report_category_harassment
    ReportCategory.CHILD_SAFETY -> R.string.report_category_child_safety
    ReportCategory.IMPERSONATION -> R.string.report_category_impersonation
    ReportCategory.OFFENSIVE_CONTENT -> R.string.report_category_offensive_content
    ReportCategory.OTHER -> R.string.report_category_other
}

@StringRes
internal fun ReportSubject.labelResource(): Int = when (this) {
    ReportSubject.MESSAGE -> R.string.report_subject_label_message
    ReportSubject.PERSON -> R.string.report_subject_label_person
    ReportSubject.NAME -> R.string.report_subject_label_name
    ReportSubject.USERNAME -> R.string.report_subject_label_username
    ReportSubject.UNKNOWN -> R.string.report_subject_label_unknown
}

@StringRes
internal fun ReportSubject.sectionResource(): Int = when (this) {
    ReportSubject.NAME -> R.string.report_section_name
    ReportSubject.USERNAME -> R.string.report_section_username
    else -> R.string.report_section_message
}

@StringRes
internal fun ReportStatus.labelResource(): Int = when (this) {
    ReportStatus.OPEN -> R.string.report_status_open
    ReportStatus.IN_REVIEW -> R.string.report_status_in_review
    ReportStatus.RESOLVED -> R.string.report_status_resolved
    ReportStatus.DISMISSED -> R.string.report_status_dismissed
}

@StringRes
internal fun ReportEventType.labelResource(): Int = when (this) {
    ReportEventType.SUBMITTED -> R.string.report_event_submitted
    ReportEventType.VIEWED -> R.string.report_event_viewed
    ReportEventType.STATUS_CHANGED -> R.string.report_event_status_changed
    ReportEventType.MESSAGE_REMOVED -> R.string.report_event_message_removed
    ReportEventType.REPORTER_REVEALED -> R.string.report_event_reporter_revealed
    ReportEventType.OTHER -> R.string.report_event_other
}

@Composable
internal fun rememberDateTimeFormatter(): (Long) -> String {
    val context = LocalContext.current
    return { epochMillis -> DateUtils.formatDateTime(context, epochMillis, DATE_TIME_FLAGS) }
}
