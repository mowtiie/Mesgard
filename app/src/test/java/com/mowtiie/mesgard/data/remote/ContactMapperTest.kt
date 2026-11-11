package com.mowtiie.mesgard.data.remote

import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.testing.contactDto
import com.mowtiie.mesgard.testing.contactEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactMapperTest {
    @Test
    fun `maps the member and the embedded profile name to a contact entity`() {
        val entity = contactDto("user", name = "Ana", role = "staff").toEntity()

        assertEquals("user", entity.userId)
        assertEquals("Ana", entity.displayName)
        assertEquals("staff", entity.role)
    }

    @Test
    fun `maps the known roles`() {
        val roles = listOf("org_admin", "staff", "student").map { contactEntity("u", role = it).toContact().role }

        assertEquals(listOf(OrgRole.ORG_ADMIN, OrgRole.STAFF, OrgRole.STUDENT), roles)
    }

    @Test
    fun `maps a role that was added later to the other role`() {
        assertEquals(OrgRole.OTHER, contactEntity("u", role = "guardian").toContact().role)
    }
}
