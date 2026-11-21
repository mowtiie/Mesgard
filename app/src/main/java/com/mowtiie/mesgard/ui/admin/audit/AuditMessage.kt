package com.mowtiie.mesgard.ui.admin.audit

import android.content.res.Resources
import androidx.annotation.StringRes
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.AuditEntry

/** A sentence for the audit log: a string resource and the words that fill it. */
internal class AuditMessage(@StringRes val resource: Int, val arguments: List<String>) {
    // A sentence has at most four words to fill in, so copying them into the vararg array costs nothing.
    @Suppress("SpreadOperator")
    fun format(resources: Resources): String = resources.getString(resource, *arguments.toTypedArray())
}

private enum class Slot {
    ACTOR,
    PERSON,
    ROLE,
    FROM_ROLE,
    TO_ROLE,
    NAME,
    FROM_NAME,
    TO_NAME,
}

private class Template(@StringRes val resource: Int, val slots: List<Slot>)

private val Templates = mapOf(
    "member.added" to Template(R.string.audit_member_added, listOf(Slot.ACTOR, Slot.PERSON, Slot.ROLE)),
    "member.removed" to Template(R.string.audit_member_removed, listOf(Slot.ACTOR, Slot.PERSON)),
    "member.role_changed" to
        Template(R.string.audit_member_role_changed, listOf(Slot.ACTOR, Slot.PERSON, Slot.FROM_ROLE, Slot.TO_ROLE)),
    "member.password_reset" to Template(R.string.audit_password_reset, listOf(Slot.ACTOR, Slot.PERSON)),
    "member.erased" to Template(R.string.audit_member_erased, listOf(Slot.ACTOR, Slot.PERSON)),
    "member.mfa_reset" to Template(R.string.audit_mfa_reset, listOf(Slot.ACTOR, Slot.PERSON)),
    "member.login_disabled" to Template(R.string.audit_login_disabled, listOf(Slot.ACTOR, Slot.PERSON)),
    "group.created" to Template(R.string.audit_group_created, listOf(Slot.ACTOR, Slot.NAME)),
    "group.renamed" to Template(R.string.audit_group_renamed, listOf(Slot.ACTOR, Slot.FROM_NAME, Slot.TO_NAME)),
    "group.deleted" to Template(R.string.audit_group_deleted, listOf(Slot.ACTOR, Slot.NAME)),
    "group_member.added" to Template(R.string.audit_group_member_added, listOf(Slot.ACTOR, Slot.PERSON)),
    "group_member.removed" to Template(R.string.audit_group_member_removed, listOf(Slot.ACTOR, Slot.PERSON)),
    "group_member.role_changed" to Template(
        R.string.audit_group_member_role_changed,
        listOf(Slot.ACTOR, Slot.PERSON, Slot.FROM_ROLE, Slot.TO_ROLE),
    ),
    "announcements.enabled" to Template(R.string.audit_announcements_enabled, listOf(Slot.ACTOR)),
    "message.pinned" to Template(R.string.audit_message_pinned, listOf(Slot.ACTOR)),
    "message.unpinned" to Template(R.string.audit_message_unpinned, listOf(Slot.ACTOR)),
    "report.reporter_revealed" to Template(R.string.audit_report_reporter_revealed, listOf(Slot.ACTOR)),
    "report_reviewers.changed" to Template(R.string.audit_report_reviewers_changed, listOf(Slot.ACTOR)),
    "organization.privacy_notice_changed" to Template(R.string.audit_privacy_notice_changed, listOf(Slot.ACTOR)),
    "legal_hold.placed" to Template(R.string.audit_legal_hold_placed, listOf(Slot.ACTOR)),
    "legal_hold.released" to Template(R.string.audit_legal_hold_released, listOf(Slot.ACTOR)),
)

/**
 * Turns a recorded action into a sentence. [names] maps people to their names, [roleLabels] maps the role names stored
 * in the log to words, and [formerMember] stands in for someone who is no longer in the organization. An action this
 * version does not know is shown by its code, so nothing in the log is hidden.
 */
internal fun describeAudit(
    entry: AuditEntry,
    names: Map<String, String>,
    roleLabels: Map<String, String>,
    formerMember: String,
): AuditMessage {
    val words = Words(entry, names, roleLabels, formerMember)
    val template =
        Templates[entry.action] ?: return AuditMessage(R.string.audit_unknown, listOf(words.actor, entry.action))
    return AuditMessage(template.resource, template.slots.map(words::of))
}

/** The words of one entry: who acted, who was affected and what the details say. */
private class Words(
    private val entry: AuditEntry,
    private val names: Map<String, String>,
    private val roleLabels: Map<String, String>,
    private val formerMember: String,
) {
    val actor: String = names[entry.actorId] ?: formerMember

    fun of(slot: Slot): String = when (slot) {
        Slot.ACTOR -> actor
        Slot.PERSON -> entry.targetId?.let { names[it] } ?: formerMember
        Slot.ROLE -> role("role")
        Slot.FROM_ROLE -> role("from")
        Slot.TO_ROLE -> role("to")
        Slot.NAME -> detail("name")
        Slot.FROM_NAME -> detail("from")
        Slot.TO_NAME -> detail("to")
    }

    private fun detail(key: String) = entry.details[key].orEmpty()

    private fun role(key: String) = roleLabels[detail(key)] ?: detail(key)
}
