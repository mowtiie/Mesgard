package com.mowtiie.mesgard.ui.admin

import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.ErasureMode
import com.mowtiie.mesgard.domain.model.OrgRole

internal enum class MemberAction {
    CHANGE_ROLE,
    RESET_PASSWORD,
    RESET_MFA,
    REMOVE,
}

internal sealed interface MembersDialog {
    data class ChangeRole(val member: Contact, val selected: OrgRole) : MembersDialog

    data class ConfirmResetPassword(val member: Contact) : MembersDialog

    data class ConfirmResetMfa(val member: Contact) : MembersDialog

    /** [erase] also erases the personal data of the person, as far as the server allows. */
    data class ConfirmRemove(val member: Contact, val erase: Boolean = false) : MembersDialog

    /** Shown once: the server does not keep the password in readable form. */
    data class TemporaryPassword(val memberName: String, val password: String) : MembersDialog
}

internal enum class MembersError {
    REFRESH_NETWORK_UNAVAILABLE,
    REFRESH_UNKNOWN,
    NETWORK_UNAVAILABLE,
    NOT_ALLOWED,
    NOT_A_MEMBER,
    LAST_ADMIN,
    NOT_ERASABLE,
    UNKNOWN,
}

internal data class MembersUiState(
    val organizationName: String? = null,
    val members: List<Contact> = emptyList(),
    val query: String = "",
    val isRefreshing: Boolean = false,
    val isWorking: Boolean = false,
    val dialog: MembersDialog? = null,
    val erasureMode: ErasureMode = ErasureMode.OFF,
    val error: MembersError? = null,
) {
    val showEmptyMessage: Boolean
        get() = members.isEmpty() && query.isBlank() && !isRefreshing

    val showNoResults: Boolean
        get() = members.isEmpty() && query.isNotBlank()
}
