package com.mowtiie.mesgard.ui.common

import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.ConversationType
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationTitlesTest {
    @Test
    fun `a direct conversation without a title belongs to someone who left`() {
        assertEquals(R.string.conversation_title_former_member, ConversationType.DIRECT.fallbackTitleResource())
    }

    @Test
    fun `other conversations keep their own fallback titles`() {
        assertEquals(R.string.conversation_title_group, ConversationType.GROUP.fallbackTitleResource())
    }

    @Test
    fun `a direct conversation explains that the other person left`() {
        assertEquals(R.string.direct_read_only, ConversationType.DIRECT.readOnlyMessageResource())
    }

    @Test
    fun `groups and unknown conversations keep the moderators only notice`() {
        assertEquals(R.string.group_read_only, ConversationType.GROUP.readOnlyMessageResource())
        assertEquals(R.string.group_read_only, (null as ConversationType?).readOnlyMessageResource())
    }
}
