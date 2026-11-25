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
    REMOVE_GROUP_PICTURE,
    RENAME_GROUP,
}

/** Null when there is nothing to take down, as for the conduct or the name of a person. */
internal fun ReportDetail.followUp(): ReportFollowUp? = when (subject) {
    ReportSubject.MESSAGE -> if (evidence != null) ReportFollowUp.REMOVE_MESSAGE else null
    ReportSubject.USERNAME -> ReportFollowUp.RESET_USERNAME
    ReportSubject.PROFILE_PICTURE -> ReportFollowUp.REMOVE_PICTURE
    ReportSubject.GROUP_PICTURE -> ReportFollowUp.REMOVE_GROUP_PICTURE
    ReportSubject.GROUP_NAME -> ReportFollowUp.RENAME_GROUP
    else -> null
}

@StringRes
internal fun ReportFollowUp.titleResource(): Int = when (this) {
    ReportFollowUp.REMOVE_MESSAGE -> R.string.report_decide_remove
    ReportFollowUp.RESET_USERNAME -> R.string.report_decide_reset_username
    ReportFollowUp.REMOVE_PICTURE -> R.string.report_decide_remove_picture
    ReportFollowUp.REMOVE_GROUP_PICTURE -> R.string.report_decide_remove_group_picture
    ReportFollowUp.RENAME_GROUP -> R.string.report_decide_rename_group
}

@StringRes
internal fun ReportFollowUp.summaryResource(): Int = when (this) {
    ReportFollowUp.REMOVE_MESSAGE -> R.string.report_decide_remove_summary
    ReportFollowUp.RESET_USERNAME -> R.string.report_decide_reset_username_summary
    ReportFollowUp.REMOVE_PICTURE -> R.string.report_decide_remove_picture_summary
    ReportFollowUp.REMOVE_GROUP_PICTURE -> R.string.report_decide_remove_group_picture_summary
    ReportFollowUp.RENAME_GROUP -> R.string.report_decide_rename_group_summary
}
