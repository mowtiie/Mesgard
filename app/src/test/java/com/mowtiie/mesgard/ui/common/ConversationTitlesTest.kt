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
        assertEquals(R.string.conversation_title_announcement, ConversationType.ANNOUNCEMENT.fallbackTitleResource())
    }

    @Test
    fun `a direct conversation explains that the other person left`() {
        assertEquals(R.string.direct_read_only, ConversationType.DIRECT.readOnlyMessageResource())
    }

    @Test
    fun `announcements and unknown conversations keep the staff only notice`() {
        assertEquals(R.string.announcement_read_only, ConversationType.ANNOUNCEMENT.readOnlyMessageResource())
        assertEquals(R.string.announcement_read_only, ConversationType.GROUP.readOnlyMessageResource())
        assertEquals(R.string.announcement_read_only, (null as ConversationType?).readOnlyMessageResource())
    }
}
