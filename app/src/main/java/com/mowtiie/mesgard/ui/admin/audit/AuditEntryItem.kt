package com.mowtiie.mesgard.ui.admin.audit

import android.text.format.DateUtils
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.AuditEntry

private const val DATE_AND_TIME =
    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH

/** The words an audit entry needs besides the names: how roles read and who a removed person is. */
internal class AuditLabels(val roleLabels: Map<String, String>, val formerMember: String)

@Composable
internal fun rememberAuditLabels(): AuditLabels = AuditLabels(
    roleLabels = mapOf(
        "org_admin" to stringResource(R.string.role_org_admin),
        "staff" to stringResource(R.string.role_staff),
        "student" to stringResource(R.string.role_member),
        "moderator" to stringResource(R.string.group_role_moderator),
        "member" to stringResource(R.string.role_member),
    ),
    formerMember = stringResource(R.string.audit_former_member),
)

/** One recorded change as a sentence with the time it happened. */
@Composable
internal fun AuditEntryItem(entry: AuditEntry, names: Map<String, String>, labels: AuditLabels) {
    val context = LocalContext.current
    val message = describeAudit(entry, names, labels.roleLabels, labels.formerMember)
    ListItem(
        headlineContent = { Text(message.format(context.resources)) },
        supportingContent = { Text(DateUtils.formatDateTime(context, entry.createdAtEpochMillis, DATE_AND_TIME)) },
    )
}
