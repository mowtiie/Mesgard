package com.mowtiie.mesgard.ui.contacts

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.Group

/** Primary tabs under the app bar. Every tab has an icon and a label, as the Material tabs guidelines ask. */
@Composable
internal fun SectionTabs(selected: ContactsSection, onSectionSelected: (ContactsSection) -> Unit) {
    PrimaryTabRow(selectedTabIndex = selected.ordinal) {
        ContactsSection.entries.forEach { section ->
            Tab(
                selected = section == selected,
                onClick = { onSectionSelected(section) },
                text = { Text(stringResource(section.labelResource())) },
                icon = { Icon(section.icon(), contentDescription = null) },
            )
        }
    }
}

/** Swiping the content changes the tab, and choosing a tab scrolls the content. */
@Composable
internal fun SectionPager(
    uiState: ContactsUiState,
    onContactClicked: (Contact) -> Unit,
    onGroupClicked: (Group) -> Unit,
    onSectionSelected: (ContactsSection) -> Unit,
) {
    val sections = ContactsSection.entries
    val pagerState = rememberPagerState(initialPage = uiState.section.ordinal) { sections.size }
    LaunchedEffect(uiState.section) {
        if (pagerState.currentPage != uiState.section.ordinal) pagerState.animateScrollToPage(uiState.section.ordinal)
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { onSectionSelected(sections[it]) }
    }
    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        ContactList(sections[page], uiState, onContactClicked, onGroupClicked)
    }
}

@StringRes
private fun ContactsSection.labelResource(): Int = when (this) {
    ContactsSection.PEOPLE -> R.string.contacts_section_people
    ContactsSection.GROUPS -> R.string.contacts_section_groups
}

@Composable
private fun ContactsSection.icon(): ImageVector = when (this) {
    ContactsSection.PEOPLE -> Icons.Default.Person
    ContactsSection.GROUPS -> ImageVector.vectorResource(R.drawable.ic_group)
}
