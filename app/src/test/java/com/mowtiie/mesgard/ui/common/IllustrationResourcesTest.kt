package com.mowtiie.mesgard.ui.common

import android.content.Context
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import com.mowtiie.mesgard.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class IllustrationResourcesTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val illustrations = listOf(
        R.drawable.illustration_chats,
        R.drawable.illustration_conversation,
        R.drawable.illustration_contacts,
        R.drawable.illustration_search,
        R.drawable.illustration_members,
        R.drawable.illustration_group_no_group,
        R.drawable.illustration_group_no_member,
        R.drawable.illustration_audit_log,
    )

    @Test
    fun `every illustration can be loaded`() {
        for (illustration in illustrations) {
            assertNotNull(ResourcesCompat.getDrawable(context.resources, illustration, context.theme))
        }
    }

    @Test
    @Config(qualifiers = "night")
    fun `every illustration can be loaded in the dark theme`() {
        for (illustration in illustrations) {
            assertNotNull(ResourcesCompat.getDrawable(context.resources, illustration, context.theme))
        }
    }

    @Test
    fun `the accent color is the brand primary in the light theme`() {
        assertEquals(LIGHT_ACCENT, context.getColor(R.color.illustration_accent))
    }

    @Test
    @Config(qualifiers = "night")
    fun `the accent color is lighter in the dark theme`() {
        assertEquals(DARK_ACCENT, context.getColor(R.color.illustration_accent))
        assertNotEquals(LIGHT_ACCENT, context.getColor(R.color.illustration_accent))
    }

    private companion object {
        const val LIGHT_ACCENT = 0xFF415F91.toInt()
        const val DARK_ACCENT = 0xFFAAC7FF.toInt()
    }
}
