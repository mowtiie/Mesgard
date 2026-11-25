package com.mowtiie.mesgard.ui.admin.groups

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.profile.picture.PictureScreen
import com.mowtiie.mesgard.ui.profile.picture.PictureTexts

private val GroupPictureTexts = PictureTexts(
    title = R.string.group_picture_title,
    hint = R.string.group_picture_hint,
    notAllowed = R.string.group_picture_not_allowed,
    removeTitle = R.string.group_picture_remove_title,
    removeMessage = R.string.group_picture_remove_message,
)

@Composable
internal fun GroupPictureScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupPictureViewModel = hiltViewModel(),
) {
    PictureScreen(viewModel, GroupPictureTexts, onBack, modifier)
}
