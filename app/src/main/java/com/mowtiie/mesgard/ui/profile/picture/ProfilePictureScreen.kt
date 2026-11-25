package com.mowtiie.mesgard.ui.profile.picture

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.mowtiie.mesgard.R

private val ProfilePictureTexts = PictureTexts(
    title = R.string.profile_picture_title,
    hint = R.string.profile_picture_hint,
    notAllowed = R.string.profile_picture_not_allowed,
    removeTitle = R.string.profile_picture_remove_title,
    removeMessage = R.string.profile_picture_remove_message,
)

@Composable
internal fun ProfilePictureScreenRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfilePictureViewModel = hiltViewModel(),
) {
    PictureScreen(viewModel, ProfilePictureTexts, onBack, modifier)
}
