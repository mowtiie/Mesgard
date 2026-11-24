package com.mowtiie.mesgard.ui.reports

import androidx.annotation.StringRes
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.ReportDetail
import com.mowtiie.mesgard.domain.model.ReportSubject

/** What a reviewer can do besides closing the report: take the reported thing down. */
internal enum class ReportFollowUp {
    REMOVE_MESSAGE,
    RESET_USERNAME,
    REMOVE_PICTURE,
}

/** Null when there is nothing to take down, as for the conduct or the name of a person. */
internal fun ReportDetail.followUp(): ReportFollowUp? = when (subject) {
    ReportSubject.MESSAGE -> if (evidence != null) ReportFollowUp.REMOVE_MESSAGE else null
    ReportSubject.USERNAME -> ReportFollowUp.RESET_USERNAME
    ReportSubject.PROFILE_PICTURE -> ReportFollowUp.REMOVE_PICTURE
    else -> null
}

@StringRes
internal fun ReportFollowUp.titleResource(): Int = when (this) {
    ReportFollowUp.REMOVE_MESSAGE -> R.string.report_decide_remove
    ReportFollowUp.RESET_USERNAME -> R.string.report_decide_reset_username
    ReportFollowUp.REMOVE_PICTURE -> R.string.report_decide_remove_picture
}

@StringRes
internal fun ReportFollowUp.summaryResource(): Int = when (this) {
    ReportFollowUp.REMOVE_MESSAGE -> R.string.report_decide_remove_summary
    ReportFollowUp.RESET_USERNAME -> R.string.report_decide_reset_username_summary
    ReportFollowUp.REMOVE_PICTURE -> R.string.report_decide_remove_picture_summary
}
