package com.mowtiie.mesgard.domain.model

internal data class AccountMembership(val organizationId: String, val organizationName: String, val role: OrgRole)

internal data class Account(val displayName: String, val email: String?, val memberships: List<AccountMembership>)
