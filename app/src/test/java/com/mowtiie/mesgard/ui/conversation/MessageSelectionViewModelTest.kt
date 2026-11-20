package com.mowtiie.mesgard.ui.conversation

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageSelectionViewModelTest {
    private val viewModel = MessageSelectionViewModel()

    @Test
    fun `nothing is selected at first`() {
        assertEquals(emptySet<String>(), viewModel.selected.value)
    }

    @Test
    fun `a message is selected and taken out again by the same action`() {
        viewModel.onToggled("m1")
        viewModel.onToggled("m2")
        assertEquals(setOf("m1", "m2"), viewModel.selected.value)

        viewModel.onToggled("m1")
        assertEquals(setOf("m2"), viewModel.selected.value)
    }

    @Test
    fun `clearing leaves the selection`() {
        viewModel.onToggled("m1")

        viewModel.onSelectionClosed()

        assertEquals(emptySet<String>(), viewModel.selected.value)
    }
}
