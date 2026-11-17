package com.mowtiie.mesgard.ui.admin.audit

import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.AuditEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class AuditMessageTest {
    private val names = mapOf("admin" to "Admin Cruz", "u1" to "Ana Reyes")
    private val roles = mapOf(
        "student" to "Student",
        "staff" to "Staff",
        "member" to "Member",
        "moderator" to "Moderator",
    )

    private fun describe(action: String, target: String? = "u1", details: Map<String, String> = emptyMap()) =
        describeAudit(AuditEntry("1", "admin", action, target, details, 0L), names, roles, "a former member")

    @Test
    fun `names the person who acted, the person affected and the role`() {
        val message = describe("member.added", details = mapOf("role" to "student"))

        assertEquals(R.string.audit_member_added, message.resource)
        assertEquals(listOf("Admin Cruz", "Ana Reyes", "Student"), message.arguments)
    }

    @Test
    fun `describes a role change with the old and the new role`() {
        val message = describe("member.role_changed", details = mapOf("from" to "student", "to" to "staff"))

        assertEquals(R.string.audit_member_role_changed, message.resource)
        assertEquals(listOf("Admin Cruz", "Ana Reyes", "Student", "Staff"), message.arguments)
    }

    @Test
    fun `uses a placeholder for someone who is no longer known`() {
        val message = describe("member.removed", target = "gone")

        assertEquals(listOf("Admin Cruz", "a former member"), message.arguments)
    }

    @Test
    fun `describes group actions with the stored names`() {
        assertEquals(
            listOf("Admin Cruz", "Chess Club"),
            describe("group.created", null, mapOf("name" to "Chess Club")).arguments,
        )
        assertEquals(
            listOf("Admin Cruz", "Chess Club", "Chess Society"),
            describe("group.renamed", null, mapOf("from" to "Chess Club", "to" to "Chess Society")).arguments,
        )
        assertEquals(R.string.audit_group_deleted, describe("group.deleted", null, mapOf("name" to "x")).resource)
    }

    @Test
    fun `describes the roles inside a group with the group role names`() {
        val message = describe("group_member.role_changed", details = mapOf("from" to "member", "to" to "moderator"))

        assertEquals(R.string.audit_group_member_role_changed, message.resource)
        assertEquals(listOf("Admin Cruz", "Ana Reyes", "Member", "Moderator"), message.arguments)
    }

    @Test
    fun `describes a reset of two-step verification`() {
        val message = describe("member.mfa_reset")

        assertEquals(R.string.audit_mfa_reset, message.resource)
        assertEquals(listOf("Admin Cruz", "Ana Reyes"), message.arguments)
    }

    @Test
    fun `shows an action it does not know instead of hiding it`() {
        val message = describe("something.new", null)

        assertEquals(R.string.audit_unknown, message.resource)
        assertEquals(listOf("Admin Cruz", "something.new"), message.arguments)
    }

    @Test
    fun `keeps a role name it has no word for`() {
        val message = describe("member.added", details = mapOf("role" to "guardian"))

        assertEquals("guardian", message.arguments.last())
    }
}
