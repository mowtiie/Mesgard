package com.mowtiie.mesgard.ui.profile.picture

import androidx.annotation.StringRes

/** The words of a picture page, which differ between the picture of a person and the picture of a group. */
internal class PictureTexts(
    @StringRes val title: Int,
    @StringRes val hint: Int,
    @StringRes val notAllowed: Int,
    @StringRes val removeTitle: Int,
    @StringRes val removeMessage: Int,
)
